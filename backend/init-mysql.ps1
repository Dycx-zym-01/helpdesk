Set-Location $PSScriptRoot

$mysqlExe = "D:\MYSQL\bin\mysql.exe"
$serviceName = "MySQL84"

if (-not (Test-Path $mysqlExe)) {
    Write-Error "mysql client was not found: $mysqlExe"
    exit 1
}

$dbHost = if ($env:HELPDESK_DB_HOST) { $env:HELPDESK_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:HELPDESK_DB_PORT_MYSQL) { $env:HELPDESK_DB_PORT_MYSQL } else { "3306" }
$dbName = if ($env:HELPDESK_DB_NAME) { $env:HELPDESK_DB_NAME } else { "helpdesk_dev" }
$dbUser = if ($env:HELPDESK_DB_USER) { $env:HELPDESK_DB_USER } else { "root" }
$dbPassword = $env:HELPDESK_DB_PASSWORD

try {
    $service = Get-Service -Name $serviceName -ErrorAction Stop
    if ($service.Status -ne "Running") {
        Write-Host "MySQL service '$serviceName' is not running."
        Write-Host "Start it as administrator, then run this script again."
        exit 1
    }
} catch {
    Write-Host "MySQL service '$serviceName' was not found. If your service name is different, just make sure MySQL is running."
}

if (-not $dbPassword) {
    $dbPassword = Read-Host "Enter MySQL password for user '$dbUser'"
}

$sql = "CREATE DATABASE IF NOT EXISTS $dbName DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

Write-Host "Creating database '$dbName' on ${dbHost}:$dbPort ..."
& $mysqlExe --host=$dbHost --port=$dbPort --protocol=TCP --user=$dbUser --password=$dbPassword -e $sql

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create database. Check MySQL host, user, password, and service status."
    exit $LASTEXITCODE
}

Write-Host "Database '$dbName' is ready."
