$ErrorActionPreference = 'Stop'

$checks = @(
    @{
        Name = 'Statistics schema and write access'
        Command = 'PGPASSWORD="$STATISTICS_DB_PASSWORD" psql -h 127.0.0.1 -U statistics_app -d statistics -v ON_ERROR_STOP=1 -c "BEGIN; CREATE TABLE statistics.connection_check (id integer PRIMARY KEY); INSERT INTO statistics.connection_check VALUES (1); SELECT * FROM statistics.connection_check; ROLLBACK;"'
        ExpectedSuccess = $true
    }
    @{
        Name = 'Parking schema and write access'
        Command = 'PGPASSWORD="$PARKING_DB_PASSWORD" psql -h 127.0.0.1 -U parking_app -d parking -v ON_ERROR_STOP=1 -c "BEGIN; CREATE TABLE parking.connection_check (id integer PRIMARY KEY); INSERT INTO parking.connection_check VALUES (1); SELECT * FROM parking.connection_check; ROLLBACK;"'
        ExpectedSuccess = $true
    }
    @{
        Name = 'Statistics cannot connect to Parking'
        Command = 'PGPASSWORD="$STATISTICS_DB_PASSWORD" psql -h 127.0.0.1 -U statistics_app -d parking -c "SELECT 1;"'
        ExpectedSuccess = $false
    }
    @{
        Name = 'Parking cannot connect to Statistics'
        Command = 'PGPASSWORD="$PARKING_DB_PASSWORD" psql -h 127.0.0.1 -U parking_app -d statistics -c "SELECT 1;"'
        ExpectedSuccess = $false
    }
    @{
        Name = 'Pharmacies schema and write access'
        Command = 'PGPASSWORD="$PHARMACIES_DB_PASSWORD" psql -h 127.0.0.1 -U pharmacies_app -d pharmacies -v ON_ERROR_STOP=1 -c "BEGIN; CREATE TABLE pharmacies.connection_check (id integer PRIMARY KEY); INSERT INTO pharmacies.connection_check VALUES (1); SELECT * FROM pharmacies.connection_check; ROLLBACK;"'
        ExpectedSuccess = $true
    }
    @{
        Name = 'Pharmacies cannot connect to Parking'
        Command = 'PGPASSWORD="$PHARMACIES_DB_PASSWORD" psql -h 127.0.0.1 -U pharmacies_app -d parking -c "SELECT 1;"'
        ExpectedSuccess = $false
    }
)

foreach ($check in $checks) {
    $output = & docker compose exec -T postgres sh -c $check.Command 2>&1
    $succeeded = $LASTEXITCODE -eq 0
    if ($succeeded -ne $check.ExpectedSuccess) {
        throw "$($check.Name) failed: $output"
    }
    if (-not $succeeded -and "$output" -notmatch 'permission denied for database') {
        throw "$($check.Name) failed for an unexpected reason: $output"
    }
    Write-Output "PASS: $($check.Name)"
}

exit 0
