Set-Location "C:\Users\admin\Desktop\projects\zentrix"
Start-Transcript -Path "C:\Users\admin\Desktop\projects\zentrix\deploy_log.txt" -Force
Write-Host "Starting deployment..."
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\target\zentrix1-0.0.1-SNAPSHOT.war" -Destination "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\zentrix1.war" -Force
Write-Host "Copied WAR file"
Start-Service -Name Tomcat10
Write-Host "Started Tomcat service"
Stop-Transcript
