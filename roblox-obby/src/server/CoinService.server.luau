--!strict
-- CoinService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Moedas colecionáveis! Partes marcadas com "Coin" giram sozinhas, e quando
-- o jogador encosta elas somem, dão pontos e voltam depois de um tempo. 🪙
--
-- Como usar no Studio: crie uma Part (dourada fica linda), marque com a tag
-- "Coin". Pronto. O contador aparece sozinho na tela.
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local RunService = game:GetService("RunService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))
local settings = Config.Coin

-- adiciona o contador "Coins" no leaderstats (criado pelo CheckpointService)
local function setupCoins(player: Player)
	local leaderstats = player:WaitForChild("leaderstats")
	if not leaderstats:FindFirstChild("Coins") then
		local coins = Instance.new("IntValue")
		coins.Name = "Coins"
		coins.Value = 0
		coins.Parent = leaderstats
	end
end

Players.PlayerAdded:Connect(setupCoins)
for _, p in Players:GetPlayers() do
	task.spawn(setupCoins, p)
end

local collected: { [BasePart]: boolean } = {}

local function connectCoin(part: BasePart)
	part.Anchored = true
	part.CanCollide = false
	part.Material = Enum.Material.Neon
	if part.Color == Color3.new(1, 1, 1) then
		part.Color = Color3.fromRGB(255, 215, 0) -- dourado por padrão
	end

	-- faz girar sem parar (efeito clássico de moeda)
	RunService.Heartbeat:Connect(function(dt)
		if part.Parent and not collected[part] then
			part.CFrame = part.CFrame * CFrame.Angles(0, math.rad(settings.spinSpeed * dt), 0)
		end
	end)

	part.Touched:Connect(function(hit)
		if collected[part] then
			return
		end
		local character = hit.Parent
		if not character then
			return
		end
		local player = Players:GetPlayerFromCharacter(character)
		if not player then
			return
		end

		collected[part] = true
		part.Transparency = 1
		part.CanCollide = false

		local leaderstats = player:FindFirstChild("leaderstats")
		local coins = leaderstats and leaderstats:FindFirstChild("Coins") :: IntValue?
		if coins then
			coins.Value += settings.value
		end

		-- volta depois de um tempo
		task.delay(settings.respawnTime, function()
			if part.Parent then
				part.Transparency = 0
				collected[part] = false
			end
		end)
	end)
end

for _, inst in CollectionService:GetTagged(Config.Tags.Coin) do
	if inst:IsA("BasePart") then
		connectCoin(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.Coin):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectCoin(inst)
	end
end)
