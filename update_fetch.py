import os, re
directory = r'c:\Users\admin\Desktop\projects\zentrix\src\main\resources\templates'
for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.html'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            content = content.replace("fetch('/", "fetch(contextPath + ('/' === contextPath ? '' : '/') + '")
            content = content.replace("window.location.href = '/", "window.location.href = contextPath + ('/' === contextPath ? '' : '/') + '")
            content = content.replace("location.href = '/", "location.href = contextPath + ('/' === contextPath ? '' : '/') + '")

            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
