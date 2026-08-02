--!strict
-- DemoLevel.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- 🌿 FASE FLORESTA 🌿
-- Monta automaticamente um obby com tema de floresta: pule de árvore em
-- árvore, escale cipós, desvie de espinhos e use cogumelos-trampolim.
--
-- Feita pra ser DIVERTIDA e POSSÍVEL de passar (nada de trecho onde tudo te
-- mata). Quando quiser fazer a sua fase do zero, mude ENABLED pra false.
-- =========================================================================

local ENABLED = true

local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))
local T = Config.Tags

if not ENABLED then
	return
end

task.wait(0.2) -- deixa os outros serviços ligarem os "ouvintes" de tag primeiro

local folder = Instance.new("Folder")
folder.Name = "DemoLevel"
folder.Parent = workspace

-- cores da floresta
local BARK = Color3.fromRGB(96, 64, 40) -- tronco
local LEAF = Color3.fromRGB(70, 150, 60) -- folhas
local LEAF2 = Color3.fromRGB(95, 175, 75) -- folhas claras
local GRASS = Color3.fromRGB(80, 140, 55) -- chão
local VINE = Color3.fromRGB(70, 120, 50) -- cipó
local THORN = Color3.fromRGB(120, 40, 60) -- espinhos (mortais)
local MUSH = Color3.fromRGB(220, 70, 70) -- cogumelo-trampolim

-- ---------- helpers ----------
local function part(pos: Vector3, size: Vector3, color: Color3, material: Enum.Material?, shape: Enum.PartType?): Part
	local p = Instance.new("Part")
	p.Anchored = true
	p.Size = size
	p.Position = pos
	p.Color = color
	p.Material = material or Enum.Material.SmoothPlastic
	if shape then
		p.Shape = shape
	end
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Parent = folder
	return p
end

-- uma árvore: tronco + copa de "bolotas" de folha. Devolve o topo da copa
-- (pra você usar como plataforma se quiser).
local function tree(base: Vector3, trunkHeight: number, canopy: Color3?, solid: boolean?): number
	local trunk = part(base + Vector3.new(0, trunkHeight / 2, 0), Vector3.new(3, trunkHeight, 3), BARK, Enum.Material.Wood)
	trunk.CanCollide = solid ~= false
	local topY = base.Y + trunkHeight
	local color = canopy or LEAF
	-- copa: várias esferas sobrepostas
	local blobs = {
		{ Vector3.new(0, 0, 0), 9 },
		{ Vector3.new(4, -1, 2), 6 },
		{ Vector3.new(-4, -1, -2), 6 },
		{ Vector3.new(2, 2, -3), 5 },
		{ Vector3.new(-2, 2, 3), 5 },
	}
	for _, b in blobs do
		local off, d = b[1] :: Vector3, b[2] :: number
		local leaf = part(base + Vector3.new(0, trunkHeight, 0) + off, Vector3.new(d, d, d), color, Enum.Material.Grass, Enum.PartType.Ball)
		leaf.CanCollide = false -- decoração, não trava o jogador
	end
	return topY
end

-- plataforma de copa em que dá pra PULAR (um disco achatado de folhas)
-- No cilindro, o eixo X é a "altura"; deixando X = 2 e girando 90°, o disco
-- fica deitado com 2 studs de espessura e "radius" de diâmetro.
local function canopyPlatform(pos: Vector3, radius: number, color: Color3?): Part
	local p = part(pos, Vector3.new(2, radius, radius), color or LEAF2, Enum.Material.Grass, Enum.PartType.Cylinder)
	p.Orientation = Vector3.new(0, 0, 90) -- deita o cilindro virando um "disco" horizontal
	return p
end

-- cipó decorativo pendurado
local function vine(top: Vector3, length: number)
	local v = part(top + Vector3.new(0, -length / 2, 0), Vector3.new(0.4, length, 0.4), VINE, Enum.Material.Grass, Enum.PartType.Cylinder)
	v.CanCollide = false
end

-- moeda (fruta dourada) flutuando
local function coin(pos: Vector3)
	local c = part(pos + Vector3.new(0, 4, 0), Vector3.new(0.4, 3, 3), Color3.fromRGB(255, 215, 0), Enum.Material.Neon)
	c.Shape = Enum.PartType.Cylinder
	c.Orientation = Vector3.new(0, 0, 90)
	c.CanCollide = false
	CollectionService:AddTag(c, T.Coin)
end

local function checkpoint(pos: Vector3, stage: number)
	local cp = part(pos, Vector3.new(8, 1, 8), Color3.fromRGB(120, 90, 60), Enum.Material.WoodPlanks)
	cp:SetAttribute("Stage", stage)
	CollectionService:AddTag(cp, T.Checkpoint)
	-- plaquinha de madeira em cima do checkpoint
	part(pos + Vector3.new(0, 2, 0), Vector3.new(1, 3, 1), BARK, Enum.Material.Wood)
end

-- ======================================================================
--  CHÃO DA FLORESTA + árvores de fundo (cenário)
-- ======================================================================
local ground = part(Vector3.new(0, -1, 75), Vector3.new(120, 2, 220), GRASS, Enum.Material.Grass)
ground.Transparency = 0

-- floresta de fundo: árvores espalhadas dos dois lados do caminho
local rng = Random.new(42)
for i = 1, 40 do
	local side = (i % 2 == 0) and 1 or -1
	local x = side * rng:NextNumber(22, 55)
	local z = rng:NextNumber(-10, 160)
	local h = rng:NextNumber(14, 30)
	local shade = (i % 3 == 0) and LEAF or LEAF2
	tree(Vector3.new(x, 0, z), h, shade, false)
end

-- ======================================================================
--  INÍCIO — clareira
-- ======================================================================
local spawn = Instance.new("SpawnLocation")
spawn.Anchored = true
spawn.Size = Vector3.new(12, 1, 12)
spawn.Position = Vector3.new(0, 5, 0)
spawn.Color = Color3.fromRGB(110, 170, 90)
spawn.Material = Enum.Material.Grass
spawn.Parent = folder
checkpoint(Vector3.new(0, 5, 0), 0)
tree(Vector3.new(-8, 0, -4), 20)
tree(Vector3.new(9, 0, -2), 24)

-- ======================================================================
--  SEÇÃO 1 — Pulando de árvore em árvore 🌳 (copas como plataformas)
-- ======================================================================
local heights = { 5, 6, 8, 6, 9 }
local z = 12
for i, hy in heights do
	tree(Vector3.new(0, hy - 6, z), 6, LEAF, false) -- tronco baixinho de apoio visual
	canopyPlatform(Vector3.new(0, hy, z), 9)
	coin(Vector3.new(0, hy, z))
	z += 9
end
checkpoint(Vector3.new(0, 9, z), 1)
z += 10

-- ======================================================================
--  SEÇÃO 2 — Escalada de cipó 🧗 (sobe pra um galho alto)
-- ======================================================================
-- galho de apoio embaixo
canopyPlatform(Vector3.new(0, 9, z), 8)
-- CIPÓ ESCALÁVEL: uma "treliça" (TrussPart) que o jogador sobe sozinho ao encostar
local truss = Instance.new("TrussPart")
truss.Anchored = true
truss.Size = Vector3.new(2, 14, 2)
truss.Position = Vector3.new(0, 17, z)
truss.Color = VINE
truss.Material = Enum.Material.Grass
truss.Parent = folder
-- cipós decorativos ao redor
vine(Vector3.new(-3, 24, z), 10)
vine(Vector3.new(3, 24, z), 12)
-- galho lá em cima, onde a escalada termina
canopyPlatform(Vector3.new(0, 24.5, z + 4), 9)
z += 4
checkpoint(Vector3.new(0, 24.5, z), 2)
z += 10

-- ======================================================================
--  SEÇÃO 3 — Ravina de espinhos + cogumelo-trampolim 🍄
-- ======================================================================
-- desce de volta pra um galho mais baixo
canopyPlatform(Vector3.new(0, 18, z), 9)
z += 9

-- fosso de ESPINHOS bem embaixo (mortal se cair)
local thorns = part(Vector3.new(0, 2, z + 6), Vector3.new(10, 3, 20), THORN, Enum.Material.Slate)
CollectionService:AddTag(thorns, T.Killer)
-- pontas de espinho decorativas
for i = 1, 6 do
	local sx = rng:NextNumber(-4, 4)
	local sz = z + rng:NextNumber(-2, 14)
	local spike = part(Vector3.new(sx, 4.5, sz), Vector3.new(1.5, 4, 1.5), THORN, Enum.Material.Slate)
	spike.Orientation = Vector3.new(0, 0, 0)
	CollectionService:AddTag(spike, T.Killer)
end

-- cogumelo-trampolim pra atravessar a ravina voando 🍄
local capBase = part(Vector3.new(0, 18.5, z), Vector3.new(2, 3, 2), Color3.fromRGB(240, 235, 210), Enum.Material.SmoothPlastic)
capBase.Shape = Enum.PartType.Cylinder
capBase.Orientation = Vector3.new(0, 0, 90)
local cap = part(Vector3.new(0, 20, z), Vector3.new(2, 8, 8), MUSH, Enum.Material.SmoothPlastic)
cap.Shape = Enum.PartType.Ball
cap.Size = Vector3.new(8, 5, 8)
CollectionService:AddTag(cap, T.JumpPad) -- pisou, VOOU!
z += 20

-- aterrissagem do outro lado da ravina
canopyPlatform(Vector3.new(0, 20, z), 10)
checkpoint(Vector3.new(0, 20, z), 3)
z += 12

-- ======================================================================
--  SEÇÃO 4 — Troncos que balançam ↔️ (plataformas móveis sobre espinhos)
-- ======================================================================
local pit = part(Vector3.new(0, 2, z + 12), Vector3.new(12, 3, 34), THORN, Enum.Material.Slate)
CollectionService:AddTag(pit, T.Killer)

-- dois troncos que vão e voltam: pule de um pro outro na hora certa
local log1 = part(Vector3.new(0, 20, z), Vector3.new(9, 2, 3), BARK, Enum.Material.Wood)
CollectionService:AddTag(log1, T.MovingPlatform)
log1:SetAttribute("Distance", 10)
log1:SetAttribute("Time", 2)
z += 12

local log2 = part(Vector3.new(0, 20, z), Vector3.new(9, 2, 3), BARK, Enum.Material.Wood)
CollectionService:AddTag(log2, T.MovingPlatform)
log2:SetAttribute("Distance", 10)
log2:SetAttribute("Time", 1.6)
z += 12

canopyPlatform(Vector3.new(0, 20, z), 10)
checkpoint(Vector3.new(0, 20, z), 4)
z += 12

-- ======================================================================
--  SEÇÃO 5 — Corrida nas folhas que somem 🍃 (com impulso de velocidade)
-- ======================================================================
local boost = part(Vector3.new(0, 20, z), Vector3.new(8, 1, 8), Color3.fromRGB(120, 220, 120), Enum.Material.Neon)
CollectionService:AddTag(boost, T.SpeedBoost)
z += 11

for i = 1, 4 do
	local leaf = canopyPlatform(Vector3.new(0, 20, z), 6, LEAF)
	CollectionService:AddTag(leaf, T.Disappearing)
	coin(Vector3.new(0, 20, z))
	z += 8
end
z += 4
checkpoint(Vector3.new(0, 20, z), 5)
z += 12

-- ======================================================================
--  CHEGADA 🏆 — casa na árvore
-- ======================================================================
tree(Vector3.new(0, 0, z + 2), 20, LEAF)
local finish = part(Vector3.new(0, 20, z), Vector3.new(14, 1, 14), Color3.fromRGB(255, 215, 0), Enum.Material.Neon)
CollectionService:AddTag(finish, T.Finish)
-- plataforma de madeira da casa na árvore
part(Vector3.new(0, 19, z), Vector3.new(16, 1, 16), Color3.fromRGB(120, 85, 55), Enum.Material.WoodPlanks)

print("[Obby] 🌿 Floresta montada! Pule pelas copas, escale o cipó e chegue na casa da árvore. 🏆")
