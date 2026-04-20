Set-Location "C:\Users\admin\Desktop\projects\zentrix"
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"

# Update Tomcat credentials
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\new_tomcat_users.xml" -Destination "C:\Program Files\Apache Software Foundation\Tomcat 10.1\conf\tomcat-users.xml" -Force

# Package Application
.\mvnw clean package -DskipTests
if (-not $?) {
    Write-Host "Maven build failed"
    Exit 1
}

# Stop service
Stop-Service -Name Tomcat10 -Force

# Deploy
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\target\zentrix1-0.0.1-SNAPSHOT.war" -Destination "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\zentrix1.war" -Force

# Start service
Start-Service -Name Tomcat10
