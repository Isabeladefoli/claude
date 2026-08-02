--!strict
-- CheckpointService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Controla os CHECKPOINTS (as bandeirinhas de salvamento) do obby.
--
-- Como usar no Studio:
--   1. Crie uma Part pra cada checkpoint.
--   2. Marque cada uma com a tag "Checkpoint".
--   3. Em cada Part, adicione um Atributo (aba Properties -> Attributes)
--      chamado "Stage" do tipo number: 0, 1, 2, 3...
--      (0 = o spawn inicial, 1 = primeiro checkpoint, e assim por diante)
--
-- O jogo cuida do resto: quando o jogador encosta num checkpoint com número
-- MAIOR que o atual, o progresso é salvo e ele passa a renascer ali.
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

-- Guarda o CFrame (posição) do checkpoint de cada jogador
local checkpointOf: { [Player]: CFrame } = {}

-- Descobre o número do estágio de uma parte de checkpoint
local function stageNumberOf(part: BasePart): number
	local attr = part:GetAttribute("Stage")
	if typeof(attr) == "number" then
		return attr
	end
	return 0
end

-- Cria o leaderstats ("Stage" aparece na tabela de placar do canto da tela)
local function setupLeaderstats(player: Player)
	local leaderstats = Instance.new("Folder")
	leaderstats.Name = "leaderstats"
	leaderstats.Parent = player

	local stage = Instance.new("IntValue")
	stage.Name = "Stage"
	stage.Value = 0
	stage.Parent = leaderstats
end

-- Teleporta o personagem pra cima do checkpoint atual
local function spawnAtCheckpoint(player: Player, character: Model)
	local cf = checkpointOf[player]
	if not cf then
		return
	end
	local root = character:WaitForChild("HumanoidRootPart", 5) :: BasePart?
	if root then
		-- +5 studs pra não nascer preso dentro da parte
		root.CFrame = cf + Vector3.new(0, 5, 0)
	end
end

local function onCharacterAdded(player: Player, character: Model)
	spawnAtCheckpoint(player, character)
end

local function onPlayerAdded(player: Player)
	checkpointOf[player] = nil
	setupLeaderstats(player)

	player.CharacterAdded:Connect(function(character)
		onCharacterAdded(player, character)
	end)
end

-- Liga o "detector de toque" num checkpoint
local function connectCheckpoint(part: BasePart)
	part.Touched:Connect(function(hit)
		local character = hit.Parent
		if not character then
			return
		end
		local humanoid = character:FindFirstChildOfClass("Humanoid")
		if not humanoid or humanoid.Health <= 0 then
			return
		end
		local player = Players:GetPlayerFromCharacter(character)
		if not player then
			return
		end

		local newStage = stageNumberOf(part)
		local stageValue = player:FindFirstChild("leaderstats")
			and player.leaderstats:FindFirstChild("Stage") :: IntValue?

		if stageValue and newStage > stageValue.Value then
			stageValue.Value = newStage
			checkpointOf[player] = part.CFrame
			-- feedbackzinho verde no checkpoint recém-alcançado
			part.Color = Color3.fromRGB(80, 220, 120)
		end
	end)
end

-- ---- Inicialização ----

Players.PlayerAdded:Connect(onPlayerAdded)
Players.PlayerRemoving:Connect(function(player)
	checkpointOf[player] = nil
end)

-- Conecta todos os checkpoints que já existem e os que forem criados depois
for _, inst in CollectionService:GetTagged(Config.Tags.Checkpoint) do
	if inst:IsA("BasePart") then
		connectCheckpoint(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.Checkpoint):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectCheckpoint(inst)
	end
end)
