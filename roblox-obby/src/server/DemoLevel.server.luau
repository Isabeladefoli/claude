--!strict
-- DemoLevel.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Monta AUTOMATICAMENTE uma fase de exemplo GRANDE e colorida quando o jogo
-- abre, usando todos os recursos: moedas, trampolim, velocidade, giratórios,
-- plataformas que se movem/somem, lava e chegada com confete.
--
-- Quando quiser construir a SUA fase do zero, mude ENABLED pra false (ou
-- apague este script) e comece a colocar seus próprios blocos + tags.
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

-- helper: cria um bloco
local function block(pos: Vector3, size: Vector3?, color: Color3?, material: Enum.Material?): Part
	local p = Instance.new("Part")
	p.Anchored = true
	p.Size = size or Vector3.new(6, 1, 6)
	p.Position = pos
	p.Color = color or Color3.fromRGB(120, 170, 255)
	p.Material = material or Enum.Material.SmoothPlastic
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Parent = folder
	return p
end

-- helper: cria uma moeda flutuando acima de uma posição
local function coin(pos: Vector3)
	local c = Instance.new("Part")
	c.Shape = Enum.PartType.Cylinder
	c.Size = Vector3.new(0.4, 3, 3)
	c.Orientation = Vector3.new(0, 0, 90) -- deita o cilindro (vira "moeda")
	c.Position = pos + Vector3.new(0, 4, 0)
	c.Anchored = true
	c.CanCollide = false
	c.Color = Color3.fromRGB(255, 215, 0)
	c.Material = Enum.Material.Neon
	c.Parent = folder
	CollectionService:AddTag(c, T.Coin)
end

-- helper: checkpoint com número de estágio
local function checkpoint(pos: Vector3, stage: number)
	local cp = block(pos, Vector3.new(8, 1, 8), Color3.fromRGB(255, 220, 90))
	cp:SetAttribute("Stage", stage)
	CollectionService:AddTag(cp, T.Checkpoint)
end

-- ======================================================================
--  INÍCIO
-- ======================================================================
local spawn = Instance.new("SpawnLocation")
spawn.Anchored = true
spawn.Size = Vector3.new(10, 1, 10)
spawn.Position = Vector3.new(0, 5, 0)
spawn.Color = Color3.fromRGB(90, 220, 130)
spawn.Material = Enum.Material.SmoothPlastic
spawn.Parent = folder
checkpoint(Vector3.new(0, 5, 0), 0)

-- ======================================================================
--  SEÇÃO 1 — Aquecimento com moedas (azul)
-- ======================================================================
local z = 10
for i = 1, 4 do
	block(Vector3.new(0, 5, z), Vector3.new(6, 1, 6), Color3.fromRGB(90, 160, 255))
	coin(Vector3.new(0, 5, z))
	z += 9
end
checkpoint(Vector3.new(0, 5, z), 1)
z += 12

-- ======================================================================
--  SEÇÃO 2 — Lava e giratório mortal (vermelho)
-- ======================================================================
-- fosso de lava largo (pule por cima com cuidado!)
local lava = block(Vector3.new(0, 4.6, z + 3), Vector3.new(6, 1, 14), Color3.fromRGB(230, 70, 50), Enum.Material.Neon)
CollectionService:AddTag(lava, T.Killer)
-- ilha no meio
block(Vector3.new(0, 5, z + 3), Vector3.new(5, 1, 5), Color3.fromRGB(120, 120, 130))
z += 16

-- barra giratória mortal (um cilindro que gira e te derruba)
local platform = block(Vector3.new(0, 5, z), Vector3.new(8, 1, 8), Color3.fromRGB(120, 120, 130))
local spinner = Instance.new("Part")
spinner.Anchored = true
spinner.Size = Vector3.new(14, 1.4, 1.4)
spinner.Position = Vector3.new(0, 6.5, z)
spinner.Color = Color3.fromRGB(255, 90, 60)
spinner.Material = Enum.Material.Neon
spinner.Parent = folder
CollectionService:AddTag(spinner, T.Spinner)
CollectionService:AddTag(spinner, T.Killer) -- gira E mata: desvie ou pule!
z += 14
checkpoint(Vector3.new(0, 5, z), 2)
z += 12

-- ======================================================================
--  SEÇÃO 3 — Trampolim e plataformas móveis (roxo)
-- ======================================================================
-- trampolim pra atravessar um vão enorme
local pad = block(Vector3.new(0, 5, z), Vector3.new(6, 1, 6), Color3.fromRGB(120, 255, 180), Enum.Material.Neon)
CollectionService:AddTag(pad, T.JumpPad)
z += 20 -- vão grande, só passa pulando alto no trampolim

-- plataforma que se move (aterrissagem)
local mover1 = block(Vector3.new(0, 9, z), Vector3.new(7, 1, 7), Color3.fromRGB(180, 120, 255))
CollectionService:AddTag(mover1, T.MovingPlatform)
mover1:SetAttribute("Distance", 12)
mover1:SetAttribute("Time", 2)
z += 16

local mover2 = block(Vector3.new(0, 9, z), Vector3.new(7, 1, 7), Color3.fromRGB(180, 120, 255))
CollectionService:AddTag(mover2, T.MovingPlatform)
mover2:SetAttribute("Distance", 10)
mover2:SetAttribute("Time", 1.3)
mover2:SetAttribute("AxisX", 0)
mover2:SetAttribute("AxisZ", 1) -- essa se move pra frente/trás
z += 16
checkpoint(Vector3.new(0, 9, z), 3)
z += 12

-- ======================================================================
--  SEÇÃO 4 — Corredor de velocidade + plataformas que somem (rosa)
-- ======================================================================
-- faixa de velocidade
local boost = block(Vector3.new(0, 9, z), Vector3.new(6, 1, 10), Color3.fromRGB(255, 240, 120), Enum.Material.Neon)
CollectionService:AddTag(boost, T.SpeedBoost)
z += 14

-- ponte de plataformas que somem (corra rápido!)
for i = 1, 4 do
	local vanish = block(Vector3.new(0, 9, z), Vector3.new(5, 1, 5), Color3.fromRGB(255, 150, 200))
	CollectionService:AddTag(vanish, T.Disappearing)
	coin(Vector3.new(0, 9, z))
	z += 8
end
z += 6
checkpoint(Vector3.new(0, 9, z), 4)
z += 12

-- ======================================================================
--  CHEGADA 🏆
-- ======================================================================
local finish = block(Vector3.new(0, 9, z), Vector3.new(12, 1, 12), Color3.fromRGB(255, 215, 0), Enum.Material.Neon)
CollectionService:AddTag(finish, T.Finish)

-- arco decorativo na chegada
for _, side in { -5, 5 } do
	local pillar = block(Vector3.new(side, 13, z), Vector3.new(1.5, 9, 1.5), Color3.fromRGB(255, 215, 0), Enum.Material.Neon)
	pillar.Anchored = true
end

print("[Obby] Fase MEGA montada! Colete moedas, use os trampolins e corra até a chegada. 🏁🎉")
