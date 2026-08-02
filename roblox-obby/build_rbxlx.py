#!/usr/bin/env python3
"""Gera o Obby.rbxlx embutindo os scripts de src/ (pra abrir com 2 cliques)."""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "src")


def read(rel):
    with open(os.path.join(SRC, rel), "r", encoding="utf-8") as f:
        return f.read()


# ordem importa só pra ficar organizado
SERVER_SCRIPTS = [
    ("LightingSetup", "server/LightingSetup.server.luau"),
    ("World", "server/World.server.luau"),
    ("VineGrab", "server/VineGrab.server.luau"),
    ("Crocodiles", "server/Crocodiles.server.luau"),
]
CLIENT_SCRIPTS = [
    ("VineClient", "client/VineClient.client.luau"),
    ("ArmPose", "client/ArmPose.client.luau"),
]

ref = [0]


def next_ref():
    ref[0] += 1
    return ref[0]


def script_item(name, source, local=False):
    cls = "LocalScript" if local else "Script"
    runctx = "" if local else '        <token name="RunContext">0</token>\n'
    # CDATA não pode conter ']]>'; nenhum script usa essa sequência.
    assert "]]>" not in source, f"{name} contém ]]> — quebraria o CDATA"
    return (
        f'    <Item class="{cls}" referent="{next_ref()}">\n'
        f"      <Properties>\n"
        f'        <string name="Name">{name}</string>\n'
        f"{runctx}"
        f'        <string name="Source"><![CDATA[{source}]]></string>\n'
        f"      </Properties>\n"
        f"    </Item>\n"
    )


out = []
out.append('<roblox version="4">\n')

# ReplicatedStorage + Shared
out.append('  <Item class="ReplicatedStorage" referent="0">\n')
out.append("    <Properties>\n")
out.append('      <string name="Name">ReplicatedStorage</string>\n')
out.append("    </Properties>\n")
out.append(f'    <Item class="Folder" referent="{next_ref()}">\n')
out.append("      <Properties>\n")
out.append('        <string name="Name">Shared</string>\n')
out.append("      </Properties>\n")
out.append("    </Item>\n")
out.append("  </Item>\n")

# ServerScriptService
out.append(f'  <Item class="ServerScriptService" referent="{next_ref()}">\n')
out.append("    <Properties>\n")
out.append('      <string name="Name">ServerScriptService</string>\n')
out.append("    </Properties>\n")
for name, path in SERVER_SCRIPTS:
    out.append(script_item(name, read(path), local=False))
out.append("  </Item>\n")

# StarterPlayer > StarterPlayerScripts
out.append(f'  <Item class="StarterPlayer" referent="{next_ref()}">\n')
out.append("    <Properties>\n")
out.append('      <string name="Name">StarterPlayer</string>\n')
out.append("    </Properties>\n")
out.append(f'    <Item class="StarterPlayerScripts" referent="{next_ref()}">\n')
out.append("      <Properties>\n")
out.append('        <string name="Name">StarterPlayerScripts</string>\n')
out.append("      </Properties>\n")
for name, path in CLIENT_SCRIPTS:
    out.append(script_item(name, read(path), local=True))
out.append("    </Item>\n")
out.append("  </Item>\n")

out.append("</roblox>\n")

with open(os.path.join(HERE, "Obby.rbxlx"), "w", encoding="utf-8") as f:
    f.write("".join(out))

print("Obby.rbxlx gerado com sucesso ✅")
