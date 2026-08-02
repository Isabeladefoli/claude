--!strict
-- KillPartService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Faz qualquer parte marcada com a tag "Killer" matar o jogador que encostar.
-- Perfeito pra lava, espinhos, água ácida, o que a criatividade mandar. 🔥
--
-- Como usar no Studio:
--   1. Crie a Part (ex: um bloco vermelho de "lava").
--   2. Marque com a tag "Killer".
--   Pronto. Encostou, morreu, e o CheckpointService renasce o jogador
--   no último checkpoint.
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

local function connectKiller(part: BasePart)
	part.Touched:Connect(function(hit)
		local character = hit.Parent
		if not character then
			return
		end
		local humanoid = character:FindFirstChildOfClass("Humanoid")
		if humanoid and humanoid.Health > 0 then
			-- só mata jogadores de verdade (evita matar NPCs por acidente,
			-- se você não quiser — basta remover esta checagem)
			if Players:GetPlayerFromCharacter(character) then
				humanoid.Health = 0
			end
		end
	end)
end

for _, inst in CollectionService:GetTagged(Config.Tags.Killer) do
	if inst:IsA("BasePart") then
		connectKiller(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.Killer):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectKiller(inst)
	end
end)
