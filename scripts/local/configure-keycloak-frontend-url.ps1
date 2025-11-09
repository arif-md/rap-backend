# Configure Keycloak to use localhost:9090 as the frontend URL
# This ensures all tokens are issued with iss=http://localhost:9090/realms/raptor

Write-Output "Configuring Keycloak frontend URL..."

# Get admin token
$tokenResponse = Invoke-RestMethod -Method Post `
    -Uri "http://localhost:9090/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=admin&password=admin&grant_type=password&client_id=admin-cli"

$adminToken = $tokenResponse.access_token

# Get current realm configuration
$realm = Invoke-RestMethod -Method Get `
    -Uri "http://localhost:9090/admin/realms/raptor" `
    -Headers @{Authorization="Bearer $adminToken"}

Write-Output "Current frontendUrl: $($realm.attributes.frontendUrl)"

# Update realm with frontendUrl
$realm.attributes = @{frontendUrl = "http://localhost:9090"}

# Save configuration
Invoke-RestMethod -Method Put `
    -Uri "http://localhost:9090/admin/realms/raptor" `
    -Headers @{
        Authorization="Bearer $adminToken"
        "Content-Type"="application/json"
    } `
    -Body ($realm | ConvertTo-Json -Depth 20) | Out-Null

Write-Output "✓ Keycloak frontendUrl set to: http://localhost:9090"
Write-Output "✓ All tokens will now use iss=http://localhost:9090/realms/raptor"
