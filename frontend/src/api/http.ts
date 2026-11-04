export async function get<T>(url: string): Promise<T> {
  const response = await fetch(url, { signal: AbortSignal.timeout(8000) });
  if (!response.ok) throw new Error("The service is temporarily unavailable.");
  return response.json() as Promise<T>;
}
