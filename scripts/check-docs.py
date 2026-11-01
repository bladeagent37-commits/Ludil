#!/usr/bin/env python3
"""Check README language navigation, local targets, branding and release-link parity."""
import argparse,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def inspect(name):
    text=(ROOT/name).read_text();assert text.splitlines()[0]=='[English](README.md) | [Türkçe](README.tr.md)',name+': language navigation'
    assert re.search(r'^# Ludil$',text,re.M),name+': official name'
    links=re.findall(r'\]\(([^)]+)\)',text)+re.findall(r'<img[^>]+src="([^"]+)"',text)
    for link in links:
        if not re.match(r'https?://',link) and not link.startswith('#'):assert (ROOT/link.split('#')[0]).exists(),name+': broken local target '+link
    visible=re.sub(r'\]\([^)]*\)',']',text);assert 'Lodil' not in visible,name+': obsolete visible branding'
    assert 'v0.3.0' in text and 'v0.4.0' in text,name+': release/development distinction'
    assert not re.search(r'releases/(?:tag|download)/v0\.4\.0',text),name+': unpublished release link'
    return text,set(links)
def main():
    p=argparse.ArgumentParser();p.add_argument('--release-metadata',type=Path);args=p.parse_args()
    english,en=inspect('README.md');turkish,tr=inspect('README.tr.md');assert en==tr,'README link sets differ'
    commands=lambda text:re.findall(r'```(?:sh|powershell)\n(.*?)```',text,re.S)
    assert commands(english)==commands(turkish),'Build/test commands differ between languages'
    assert all(english.count(value)==turkish.count(value) for value in ['v0.3.0','v0.4.0','Ctrl+Shift+F8','Ctrl+Shift+F9','Ctrl+Shift+F10','10.0.403','600']),'Version, requirement or shortcut mismatch'
    assert not (ROOT/'.github/workflows').exists(),'Unexpected Actions workflow directory'
    if args.release_metadata:
        releases=json.loads(args.release_metadata.read_text());published={r['tag']:r for r in releases if not r.get('draft')};assert 'v0.3.0' in published and 'v0.4.0' not in published
        assets={item['url'] for item in published['v0.3.0']['assets']}
        for link in en:
            if '/releases/download/' in link:assert link in assets,'README asset is not in the real release: '+link
    print('README CHECK PASS: language navigation, valid local links, matching external links and commands, current release/development status, visible Ludil branding, no Actions')
if __name__=='__main__':main()
