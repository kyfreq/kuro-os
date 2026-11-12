"""Builds app/src/main/assets/index.html from the browser prototype (web/prototype.html)."""
import sys, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
s = (root / "web/prototype.html").read_text()
head = '''<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<script>if(window.Android)document.documentElement.classList.add("native")</script>
<style>
[hidden]{display:none!important}img{max-width:100%}
html.native,html.native body{background:transparent!important;background-image:none!important;margin:0;padding:0!important;width:100%;height:100%;overflow:hidden;-webkit-tap-highlight-color:transparent}
html.native body{position:fixed;inset:0}
html.native .wrap,html.native .phone{display:block;width:100%;height:100%;max-width:none;margin:0;padding:0;border-radius:0;background:none;box-shadow:none}
html.native .screen{width:100%;height:100%;border-radius:0;background:var(--wall,none) center/cover no-repeat}
html.native .screen::before,html.native .status,html.native .notes{display:none}
html.native .pages{touch-action:pan-x}
html.native .pgin{padding-top:8px}
html.native .drawer{top:8px}
</style>
'''
s = s.replace('<meta name="claude:viewport-fit-sides" content="cover">\n', '')
i = s.index('</style>') + len('</style>')
out = head + s[:i] + '\n</head><body>\n' + s[i:] + '\n</body></html>\n'
(root / "app/src/main/assets/index.html").write_text(out)
print("wrote assets/index.html")
