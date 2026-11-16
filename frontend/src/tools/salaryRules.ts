/**
 * Employee pay in Cyprus for 2026: Social Insurance, GESY (General Healthcare System) and income tax.
 * Update every January: the insurable earnings ceiling changes yearly, the tax bands when the law changes.
 */
export const salaryRules = {
  year: 2026,
  /** Employee Social Insurance rate (8.8%) up to the maximum insurable earnings, €5,742 a month in 2026. */
  socialInsuranceRate: 0.088,
  socialInsuranceCeiling: 5_742 * 12,
  /** Employee GESY contribution on earnings up to €180,000 a year. */
  gesyRate: 0.0265,
  gesyCeiling: 180_000,
  /** Contributions are deducted from taxable income up to a fifth of it. */
  contributionDeductionShare: 0.2,
  /** Tax bands from 1 January 2026: the rate applies to the part of taxable income above `from`. */
  taxBands: [
    { from: 0, rate: 0 },
    { from: 22_000, rate: 0.2 },
    { from: 32_000, rate: 0.25 },
    { from: 42_000, rate: 0.3 },
    { from: 72_000, rate: 0.35 },
  ],
  /**
   * Exemptions for a first job in Cyprus after living abroad (Income Tax Law, article 8(23A) and 8(21A)):
   * half of the salary for up to 17 years when it is above €55,000 a year, otherwise a fifth of it, at most
   * €8,550 a year, for up to 7 years. They lower income tax only, not the contributions.
   */
  newResidentHalfAbove: 55_000,
  newResidentHalfShare: 0.5,
  newResidentFifthShare: 0.2,
  newResidentFifthCap: 8_550,
} as const;

/** Which new-resident exemption a yearly gross salary gets. */
export function newResidentExemption(gross: number): "half" | "fifth" {
  return gross > salaryRules.newResidentHalfAbove ? "half" : "fifth";
}

export type SalaryBreakdown = {
  gross: number;
  socialInsurance: number;
  gesy: number;
  /** Salary left out of income tax by the new-resident exemption, 0 without it. */
  exemption: number;
  taxableIncome: number;
  incomeTax: number;
  net: number;
};

const cents = (value: number) => Math.round(value * 100) / 100;

/** Yearly amounts for a yearly gross salary in euros, optionally with the new-resident exemption. */
export function netSalary(grossPerYear: number, newResident = false): SalaryBreakdown {
  const gross = Math.max(0, grossPerYear);
  const rules = salaryRules;
  const socialInsurance = cents(Math.min(gross, rules.socialInsuranceCeiling) * rules.socialInsuranceRate);
  const gesy = cents(Math.min(gross, rules.gesyCeiling) * rules.gesyRate);
  const exemption = !newResident
    ? 0
    : newResidentExemption(gross) === "half"
      ? cents(gross * rules.newResidentHalfShare)
      : Math.min(cents(gross * rules.newResidentFifthShare), rules.newResidentFifthCap);
  const deduction = Math.min(socialInsurance + gesy, gross * rules.contributionDeductionShare);
  const taxableIncome = cents(Math.max(0, gross - exemption - deduction));
  let incomeTax = 0;
  rules.taxBands.forEach((band, index) => {
    const next = rules.taxBands[index + 1]?.from ?? Infinity;
    if (taxableIncome > band.from) incomeTax += (Math.min(taxableIncome, next) - band.from) * band.rate;
  });
  incomeTax = cents(incomeTax);
  return {
    gross,
    socialInsurance,
    gesy,
    exemption,
    taxableIncome,
    incomeTax,
    net: cents(gross - socialInsurance - gesy - incomeTax),
  };
}
