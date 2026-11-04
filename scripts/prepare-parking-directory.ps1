param(
    [Parameter(Mandatory = $true)][string]$InputPath,
    [string]$OutputPath = "$PSScriptRoot/../backend/parking-service/src/main/resources/parking-directory.json"
)
$ErrorActionPreference = 'Stop'
$settings = [Xml.XmlReaderSettings]::new()
$settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
$settings.XmlResolver = $null
$reader = [Xml.XmlReader]::Create((Resolve-Path -LiteralPath $InputPath).Path, $settings)
try {
    $xml = [Xml.XmlDocument]::new()
    $xml.XmlResolver = $null
    $xml.Load($reader)
} finally { $reader.Dispose() }
$ns = [Xml.XmlNamespaceManager]::new($xml.NameTable)
$ns.AddNamespace('p', 'http://datex2.eu/schema/3/parking')
$ns.AddNamespace('c', 'http://datex2.eu/schema/3/common')
$records = @($xml.SelectNodes('/p:ParkingFacilitiesPublication/p:parkingFacility', $ns) | ForEach-Object {
    $name = $_.SelectSingleNode('p:parkingFacilityName[c:lang="en"]/c:value', $ns).InnerText
    if ($name -match '[^\x20-\x7E]' -or $name -match '^OSM Origin$' -or [string]::IsNullOrWhiteSpace($name)) { $name = $null }
    [ordered]@{
        id = 'cynap-' + $_.SelectSingleNode('c:versionedIdentifier/c:identifier', $ns).InnerText
        name = $name
        latitude = [double]::Parse($_.SelectSingleNode('.//c:latitude', $ns).InnerText, [Globalization.CultureInfo]::InvariantCulture)
        longitude = [double]::Parse($_.SelectSingleNode('.//c:longitude', $ns).InnerText, [Globalization.CultureInfo]::InvariantCulture)
    }
})
if ($records.Count -eq 0) { throw 'No parking facilities found' }
$directory = [ordered]@{
    source = 'Cyprus National Access Point / Public Works Department'
    catalogUrl = 'https://www.traffic4cyprus.org.cy/en/dataset/parking_areas'
    resourceUrl = 'https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/'
    license = 'CC BY 4.0'
    licenseUrl = 'https://creativecommons.org/licenses/by/4.0/'
    publishedAt = $xml.SelectSingleNode('/p:ParkingFacilitiesPublication/c:publicationTime', $ns).InnerText
    retrievedAt = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
    transformations = 'English names only. Missing/non-English names and OSM Origin placeholders become unnamed car parks. Coordinates and source identifiers are preserved. No occupancy data is included.'
    items = $records
}
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($directory | ConvertTo-Json -Depth 8) + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Prepared $($records.Count) locations. Review the snapshot and source terms before committing."
