Stop-Service -Name Tomcat10 -Force

Copy-Item "C:\Users\admin\Desktop\projects\zentrix\tomcat_server.xml" -Destination "C:\Program Files\Apache Software Foundation\Tomcat 10.1\conf\server.xml" -Force
Copy-Item "C:\Users\admin\Desktop\projects\zentrix\manager_web.xml" -Destination "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\manager\WEB-INF\web.xml" -Force

Start-Service -Name Tomcat10
