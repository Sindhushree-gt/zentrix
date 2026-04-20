import os, re
directory = r'c:\Users\admin\Desktop\projects\zentrix\src\main\resources\templates'
for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.html'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            content = re.sub(r'href="/([^"]+)"', r'th:href="@{/\1}"', content)
            content = re.sub(r'src="/([^"]+)"', r'th:src="@{/\1}"', content)
            content = re.sub(r'action="/([^"]+)"', r'th:action="@{/\1}"', content)
            
            # Revert any th:th:href that might have happened
            content = content.replace('th:th:href', 'th:href')
            content = content.replace('th:th:src', 'th:src')
            content = content.replace('th:th:action', 'th:action')
            
            # Since the user explicitly requested ${pageContext.request.contextPath},
            # let's inject a global script variable at the end of <head> or at the top of <body>
            # so their javascript fetches using absolute URLs will still break unless they use it.
            # But the user themselves provided the JSP snippet. Let's provide a global JS variable 
            # defined in a standard thymeleaf way so they can use it.
            context_script = """<script th:inline="javascript">
    var contextPath = /*[[@{/}]]*/ '';
    if(contextPath === '/') contextPath = '';
</script>"""
            if '<head>' in content and 'var contextPath' not in content:
                content = content.replace('<head>', '<head>\n    ' + context_script)
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
