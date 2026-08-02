--!strict
-- DisappearingPlatformService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Plataformas marcadas com "Disappearing" somem quando o jogador pisa,
-- e voltam depois de um tempo. Aquele frio na barriga do obby. 😅
--
-- Como usar no Studio:
--   1. Crie a Part (Anchored = true).
--   2. Marque com a tag "Disappearing".
--   Tempos de sumiço/volta ficam no Config.luau.
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))
local settings = Config.Disappearing

-- evita disparar várias vezes enquanto a plataforma já está sumindo
local vanishing: { [BasePart]: boolean } = {}

local function vanishAndReturn(part: BasePart)
	if vanishing[part] then
		return
	end
	vanishing[part] = true

	local originalTransparency = part.Transparency

	-- 1) aviso: fica meio transparente
	part.Transparency = settings.warningTransparency
	task.wait(settings.delayBeforeVanish)

	-- 2) some de vez (invisível e sem colisão)
	part.Transparency = 1
	part.CanCollide = false

	-- 3) espera e volta ao normal
	task.wait(settings.respawnTime)
	part.Transparency = originalTransparency
	part.CanCollide = true

	vanishing[part] = false
end

local function connectPlatform(part: BasePart)
	part.Touched:Connect(function(hit)
		local character = hit.Parent
		if not character then
			return
		end
		local humanoid = character:FindFirstChildOfClass("Humanoid")
		if humanoid and humanoid.Health > 0 and Players:GetPlayerFromCharacter(character) then
			task.spawn(vanishAndReturn, part)
		end
	end)
end

for _, inst in CollectionService:GetTagged(Config.Tags.Disappearing) do
	if inst:IsA("BasePart") then
		connectPlatform(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.Disappearing):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectPlatform(inst)
	end
end)
