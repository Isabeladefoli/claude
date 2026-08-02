--!strict
-- MovingPlatformService.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Faz plataformas marcadas com "MovingPlatform" irem e voltarem sozinhas.
-- Ótimo pra deixar o obby mais desafiador (timing!).
--
-- Como usar no Studio:
--   1. Crie a Part e deixe ela "Anchored" (Ancorada) = true.
--   2. Marque com a tag "MovingPlatform".
--   Distância, velocidade e direção você ajusta no Config.luau,
--   ou por parte usando os Atributos opcionais:
--      "Distance" (number), "Time" (number), "AxisX/AxisY/AxisZ" (number)
-- =========================================================================

local CollectionService = game:GetService("CollectionService")
local TweenService = game:GetService("TweenService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("Shared"):WaitForChild("Config"))

local function numberAttr(part: BasePart, name: string, default: number): number
	local v = part:GetAttribute(name)
	return typeof(v) == "number" and v or default
end

local function startMoving(part: BasePart)
	-- garante que fica ancorada, senão a física briga com o tween
	part.Anchored = true

	local distance = numberAttr(part, "Distance", Config.MovingPlatform.distance)
	local moveTime = numberAttr(part, "Time", Config.MovingPlatform.time)

	local axis = Vector3.new(
		numberAttr(part, "AxisX", Config.MovingPlatform.axis.X),
		numberAttr(part, "AxisY", Config.MovingPlatform.axis.Y),
		numberAttr(part, "AxisZ", Config.MovingPlatform.axis.Z)
	)

	local startCFrame = part.CFrame
	local goalCFrame = startCFrame + (axis * distance)

	local info = TweenInfo.new(
		moveTime,
		Enum.EasingStyle.Sine,
		Enum.EasingDirection.InOut,
		-1, -- repete pra sempre
		true -- reverse: volta sozinha (efeito vai-e-volta)
	)

	local tween = TweenService:Create(part, info, { CFrame = goalCFrame })
	tween:Play()
end

for _, inst in CollectionService:GetTagged(Config.Tags.MovingPlatform) do
	if inst:IsA("BasePart") then
		startMoving(inst)
	end
end
CollectionService:GetInstanceAddedSignal(Config.Tags.MovingPlatform):Connect(function(inst)
	if inst:IsA("BasePart") then
		startMoving(inst)
	end
end)
