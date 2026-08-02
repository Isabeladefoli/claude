--!strict
-- GimmicksService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Junta três "brinquedinhos" que deixam o obby MUITO mais divertido:
--   🚀 JumpPad    -> trampolim, lança o jogador pro alto
--   ⚡ SpeedBoost -> deixa o jogador super rápido por alguns segundos
--   🌀 Spinner    -> parte que gira sem parar (combine com "Killer" pra virar
--                    um obstáculo mortal de derrubar!)
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local RunService = game:GetService("RunService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

-- ---------------------------------------------------------------- JumpPad
local function connectJumpPad(part: BasePart)
	part.Anchored = true
	local debounce: { [Model]: boolean } = {}
	part.Touched:Connect(function(hit)
		local character = hit.Parent :: Model?
		if not character or debounce[character] then
			return
		end
		local humanoid = character:FindFirstChildOfClass("Humanoid")
		local root = character:FindFirstChild("HumanoidRootPart") :: BasePart?
		if humanoid and root and humanoid.Health > 0 and Players:GetPlayerFromCharacter(character) then
			debounce[character] = true
			root.AssemblyLinearVelocity = Vector3.new(
				root.AssemblyLinearVelocity.X,
				Config.JumpPad.power,
				root.AssemblyLinearVelocity.Z
			)
			task.delay(0.3, function()
				debounce[character] = nil
			end)
		end
	end)
end

-- ------------------------------------------------------------- SpeedBoost
local function connectSpeedBoost(part: BasePart)
	part.Anchored = true
	local debounce: { [Humanoid]: boolean } = {}
	part.Touched:Connect(function(hit)
		local character = hit.Parent
		if not character then
			return
		end
		local humanoid = character:FindFirstChildOfClass("Humanoid")
		if not humanoid or humanoid.Health <= 0 or debounce[humanoid] then
			return
		end
		if not Players:GetPlayerFromCharacter(character) then
			return
		end
		debounce[humanoid] = true
		local original = humanoid.WalkSpeed
		humanoid.WalkSpeed = Config.SpeedBoost.walkSpeed
		task.delay(Config.SpeedBoost.duration, function()
			if humanoid.Parent then
				humanoid.WalkSpeed = original
			end
			debounce[humanoid] = nil
		end)
	end)
end

-- ---------------------------------------------------------------- Spinner
local function connectSpinner(part: BasePart)
	part.Anchored = true
	local axis = Config.Spinner.axis
	RunService.Heartbeat:Connect(function(dt)
		if part.Parent then
			local angle = math.rad(Config.Spinner.speed * dt)
			part.CFrame = part.CFrame * CFrame.Angles(axis.X * angle, axis.Y * angle, axis.Z * angle)
		end
	end)
end

-- helper genérico: liga um conector em todas as partes de uma tag (agora e no futuro)
local function bind(tag: string, fn: (BasePart) -> ())
	for _, inst in CollectionService:GetTagged(tag) do
		if inst:IsA("BasePart") then
			fn(inst)
		end
	end
	CollectionService:GetInstanceAddedSignal(tag):Connect(function(inst)
		if inst:IsA("BasePart") then
			fn(inst)
		end
	end)
end

bind(Config.Tags.JumpPad, connectJumpPad)
bind(Config.Tags.SpeedBoost, connectSpeedBoost)
bind(Config.Tags.Spinner, connectSpinner)
