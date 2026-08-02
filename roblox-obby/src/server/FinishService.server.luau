--!strict
-- FinishService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Detecta quando o jogador chega na parte marcada com "Finish" e avisa
-- o cliente pra mostrar a tela de "VOCÊ VENCEU!". 🏆
--
-- Como usar no Studio:
--   1. Crie a Part da chegada.
--   2. Marque com a tag "Finish".
--   O RemoteEvent "ObbyFinished" é criado automaticamente em ReplicatedStorage.
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

-- cria (ou reaproveita) o RemoteEvent usado pra avisar o cliente
local finishedEvent = ReplicatedStorage:FindFirstChild("ObbyFinished") :: RemoteEvent?
if not finishedEvent then
	finishedEvent = Instance.new("RemoteEvent")
	finishedEvent.Name = "ObbyFinished"
	finishedEvent.Parent = ReplicatedStorage
end

-- pra não spammar o cliente enquanto o jogador fica em cima da chegada
local alreadyWon: { [Player]: boolean } = {}

Players.PlayerRemoving:Connect(function(player)
	alreadyWon[player] = nil
end)

local function connectFinish(part: BasePart)
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
		if not player or alreadyWon[player] then
			return
		end

		alreadyWon[player] = true
		local event = finishedEvent :: RemoteEvent
		event:FireClient(player)

		-- libera de novo depois de alguns segundos, caso queira rejogar
		task.delay(5, function()
			alreadyWon[player] = false
		end)
	end)
end

for _, inst in CollectionService:GetTagged(Config.Tags.Finish) do
	if inst:IsA("BasePart") then
		connectFinish(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.Finish):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectFinish(inst)
	end
end)
