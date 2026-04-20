Set-Location "C:\Users\admin\Desktop\projects\zentrix"
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$tomcatDir = "C:\Program Files\Apache Software Foundation\Tomcat 10.1"

Write-Host "--- Starting Full Deployment ---"

# 1. Update tomcat-users.xml
Write-Host "Updating tomcat-users.xml..."
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\new_tomcat_users.xml" -Destination "$tomcatDir\conf\tomcat-users.xml" -Force

# 2. Update server.xml (for file upload capacity)
Write-Host "Updating server.xml..."
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\tomcat_server.xml" -Destination "$tomcatDir\conf\server.xml" -Force

# 3. Disable Manager context IP restriction (for 'taking access')
Write-Host "Updating Manager context.xml..."
$managerContext = "$tomcatDir\webapps\manager\META-INF\context.xml"
if (Test-Path $managerContext) {
    (Get-Content $managerContext) -replace '<Valve ', '<!-- <Valve ' -replace '" />', '" /> -->' | Set-Content $managerContext
}

$hostContext = "$tomcatDir\webapps\host-manager\META-INF\context.xml"
if (Test-Path $hostContext) {
    (Get-Content $hostContext) -replace '<Valve ', '<!-- <Valve ' -replace '" />', '" /> -->' | Set-Content $hostContext
}

# 4. Package Application
Write-Host "Building project..."
.\mvnw.cmd clean package -DskipTests
if (-not $?) {
    Write-Host "Maven build failed"
    Exit 1
}

# 5. Stop service
Write-Host "Stopping Tomcat..."
Stop-Service -Name Tomcat10 -ErrorAction SilentlyContinue

# 6. Deploy
Write-Host "Deploying WAR..."
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\target\zentrix1-0.0.1-SNAPSHOT.war" -Destination "$tomcatDir\webapps\zentrix1.war" -Force

# 7. Start service
Write-Host "Starting Tomcat..."
Start-Service -Name Tomcat10

Write-Host "--- Deployment Complete ---"
Write-Host "Access it at: http://localhost:8080/zentrix1"
Write-Host "Manager Credentials: admin / adminpassword123"
