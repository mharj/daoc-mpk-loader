daoc-mpk-loader
===============

Simple Dark Age of Camelot java mpk file loader class.
Loosely based from original Oliver Jowett dempak.c utility.

<table>
<tr><th>MPK parts</th></tr>
<tr><td>mpk name (stage 0)</td></tr>
<tr><td>file index list (stage 1)</td></tr>
<tr><td>...</td></tr>
<tr><td>...</td></tr>
</table>
<br/>
<table>
<tr><th>One Stage 1 index</th></tr>
<tr><td>name</td><td>char[256]</td></tr>
<tr><td>timestamp(unix)</td><td>int</td></tr>
<tr><td>??? (attrs?)</td><td>int</td></tr>
<tr><td>uncompressed seek</td><td>int</td></tr>
<tr><td>uncompressed size</td><td>int</td></tr>
<tr><td>compressed seek (pos after stage 1)</td><td>int</td></tr>
<tr><td>compressed size</td><td>int</td></tr>
<tr><td>??? (sum?)</td><td>int</td></tr>
</table>