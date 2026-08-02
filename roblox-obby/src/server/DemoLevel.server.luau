--!strict
-- DemoLevel.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Monta AUTOMATICAMENTE uma fase de exemplo jogável quando o jogo abre.
-- É só pra você ter algo pronto no primeiro Play e entender como tudo se
-- encaixa. Quando quiser construir a SUA fase de verdade, é só apagar este
-- script (ou deixar a variável ENABLED como false aqui embaixo).
-- =========================================================================

local ENABLED = true -- mude pra false quando for construir sua própria fase

local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

if not ENABLED then
	return
end

-- espera um instante pra garantir que os outros serviços já ligaram os
-- "ouvintes" de tags antes da gente criar as partes
task.wait(0.2)

local folder = Instance.new("Folder")
folder.Name = "DemoLevel"
folder.Parent = workspace

-- helper: cria um bloco no mundo
local function block(props: { [string]: any }): Part
	local p = Instance.new("Part")
	p.Anchored = true
	p.Size = props.Size or Vector3.new(6, 1, 6)
	p.Position = props.Position or Vector3.new(0, 5, 0)
	p.Color = props.Color or Color3.fromRGB(120, 170, 255)
	p.Material = props.Material or Enum.Material.SmoothPlastic
	p.Parent = folder
	return p
end

-- ponto de partida (spawn)
local spawn = Instance.new("SpawnLocation")
spawn.Anchored = true
spawn.Size = Vector3.new(8, 1, 8)
spawn.Position = Vector3.new(0, 5, 0)
spawn.Color = Color3.fromRGB(90, 220, 130)
spawn.Parent = folder

-- checkpoint 0 (o começo) -> fica junto do spawn
local cp0 = block({ Position = Vector3.new(0, 5, 0), Size = Vector3.new(8, 1, 8), Color = Color3.fromRGB(90, 220, 130) })
cp0:SetAttribute("Stage", 0)
CollectionService:AddTag(cp0, Config.Tags.Checkpoint)

-- alguns degraus pra frente (eixo Z crescente)
block({ Position = Vector3.new(0, 5, 10) })
block({ Position = Vector3.new(0, 5, 20) })

-- lava no meio do caminho (pule por cima!)
local lava = block({ Position = Vector3.new(0, 5, 30), Size = Vector3.new(6, 1, 6), Color = Color3.fromRGB(230, 70, 50), Material = Enum.Material.Neon })
CollectionService:AddTag(lava, Config.Tags.Killer)

-- plataforma do outro lado da lava
block({ Position = Vector3.new(0, 5, 40) })

-- checkpoint 1
local cp1 = block({ Position = Vector3.new(0, 5, 50), Color = Color3.fromRGB(255, 220, 90) })
cp1:SetAttribute("Stage", 1)
CollectionService:AddTag(cp1, Config.Tags.Checkpoint)

-- plataforma que se move (vai e volta no eixo X)
local mover = block({ Position = Vector3.new(0, 5, 62), Color = Color3.fromRGB(180, 120, 255) })
CollectionService:AddTag(mover, Config.Tags.MovingPlatform)

-- plataforma que some quando você pisa
local vanish = block({ Position = Vector3.new(0, 5, 74), Color = Color3.fromRGB(255, 150, 200) })
CollectionService:AddTag(vanish, Config.Tags.Disappearing)

-- checkpoint 2
local cp2 = block({ Position = Vector3.new(0, 5, 84), Color = Color3.fromRGB(255, 220, 90) })
cp2:SetAttribute("Stage", 2)
CollectionService:AddTag(cp2, Config.Tags.Checkpoint)

-- linha de chegada 🏆
local finish = block({ Position = Vector3.new(0, 5, 94), Size = Vector3.new(10, 1, 10), Color = Color3.fromRGB(255, 215, 0), Material = Enum.Material.Neon })
CollectionService:AddTag(finish, Config.Tags.Finish)

print("[Obby] Fase de exemplo montada! Aperte Play e pule até a chegada. 🏁")
