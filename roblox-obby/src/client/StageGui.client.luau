--!strict
-- StageGui.client.luau  ->  vai em StarterPlayer > StarterPlayerScripts
-- =========================================================================
-- Interface do jogo, tudo feito por código:
--   * "Estágio: N"  no topo
--   * "🪙 Moedas: N" logo abaixo
--   * "⏱️ Tempo: N s" cronômetro correndo
--   * tela grande de "VOCÊ VENCEU!" com CONFETE quando chega no Finish 🎉
-- =========================================================================

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local TweenService = game:GetService("TweenService")

local player = Players.LocalPlayer

-- ---- ScreenGui base ----
local gui = Instance.new("ScreenGui")
gui.Name = "ObbyGui"
gui.ResetOnSpawn = false
gui.IgnoreGuiInset = true
gui.ZIndexBehavior = Enum.ZIndexBehavior.Sibling
gui.Parent = player:WaitForChild("PlayerGui")

-- helper pra criar os "cartõezinhos" de informação no topo
local function makeInfoCard(order: number, text: string): TextLabel
	local label = Instance.new("TextLabel")
	label.AnchorPoint = Vector2.new(0.5, 0)
	label.Position = UDim2.new(0.5, 0, 0.02 + (order * 0.065), 0)
	label.Size = UDim2.new(0, 240, 0, 46)
	label.BackgroundColor3 = Color3.fromRGB(18, 18, 28)
	label.BackgroundTransparency = 0.2
	label.TextColor3 = Color3.fromRGB(255, 255, 255)
	label.Font = Enum.Font.GothamBold
	label.TextScaled = true
	label.Text = text
	label.Parent = gui

	local corner = Instance.new("UICorner")
	corner.CornerRadius = UDim.new(0, 12)
	corner.Parent = label

	local pad = Instance.new("UIPadding")
	pad.PaddingLeft = UDim.new(0, 14)
	pad.PaddingRight = UDim.new(0, 14)
	pad.Parent = label

	local stroke = Instance.new("UIStroke")
	stroke.Color = Color3.fromRGB(120, 140, 255)
	stroke.Thickness = 1.5
	stroke.Transparency = 0.3
	stroke.Parent = label

	return label
end

local stageLabel = makeInfoCard(0, "Estágio: 0")
local coinLabel = makeInfoCard(1, "🪙 Moedas: 0")
local timeLabel = makeInfoCard(2, "⏱️ Tempo: 0.0s")

-- ---- segue os valores do leaderstats ----
task.spawn(function()
	local leaderstats = player:WaitForChild("leaderstats")

	local stage = leaderstats:WaitForChild("Stage") :: IntValue
	local function updStage()
		stageLabel.Text = "Estágio: " .. tostring(stage.Value)
	end
	updStage()
	stage.Changed:Connect(updStage)

	local coins = leaderstats:WaitForChild("Coins") :: IntValue
	local function updCoins()
		coinLabel.Text = "🪙 Moedas: " .. tostring(coins.Value)
	end
	updCoins()
	coins.Changed:Connect(updCoins)
end)

-- ---- cronômetro ----
local startTime = os.clock()
local won = false
task.spawn(function()
	while true do
		if not won then
			timeLabel.Text = string.format("⏱️ Tempo: %.1fs", os.clock() - startTime)
		end
		task.wait(0.1)
	end
end)

-- ---- confete 🎉 ----
local function burstConfetti()
	local character = player.Character
	local root = character and character:FindFirstChild("HumanoidRootPart") :: BasePart?
	if not root then
		return
	end

	local attachment = Instance.new("Attachment")
	attachment.Parent = root

	local confetti = Instance.new("ParticleEmitter")
	confetti.Rate = 0
	confetti.Lifetime = NumberRange.new(1.5, 3)
	confetti.Speed = NumberRange.new(20, 40)
	confetti.SpreadAngle = Vector2.new(180, 180)
	confetti.Rotation = NumberRange.new(0, 360)
	confetti.RotSpeed = NumberRange.new(-200, 200)
	confetti.Size = NumberSequence.new(0.4)
	confetti.Acceleration = Vector3.new(0, -30, 0)
	confetti.Color = ColorSequence.new({
		ColorSequenceKeypoint.new(0, Color3.fromRGB(255, 80, 120)),
		ColorSequenceKeypoint.new(0.5, Color3.fromRGB(90, 200, 255)),
		ColorSequenceKeypoint.new(1, Color3.fromRGB(255, 220, 90)),
	})
	confetti.Parent = attachment
	confetti:Emit(150)

	task.delay(4, function()
		attachment:Destroy()
	end)
end

-- ---- tela de vitória ----
local function showWinScreen()
	won = true
	burstConfetti()

	local finalTime = os.clock() - startTime
	local coinsValue = 0
	local leaderstats = player:FindFirstChild("leaderstats")
	if leaderstats and leaderstats:FindFirstChild("Coins") then
		coinsValue = (leaderstats.Coins :: IntValue).Value
	end

	local win = Instance.new("TextLabel")
	win.AnchorPoint = Vector2.new(0.5, 0.5)
	win.Position = UDim2.new(0.5, 0, 0.5, 0)
	win.Size = UDim2.new(0, 560, 0, 200)
	win.BackgroundColor3 = Color3.fromRGB(28, 190, 120)
	win.TextColor3 = Color3.fromRGB(255, 255, 255)
	win.Font = Enum.Font.GothamBlack
	win.TextScaled = true
	win.Text = string.format("🏆 VOCÊ VENCEU! 🏆\n⏱️ %.1fs   •   🪙 %d moedas", finalTime, coinsValue)
	win.BackgroundTransparency = 1
	win.TextTransparency = 1
	win.Parent = gui

	local winCorner = Instance.new("UICorner")
	winCorner.CornerRadius = UDim.new(0, 24)
	winCorner.Parent = win

	local winStroke = Instance.new("UIStroke")
	winStroke.Color = Color3.fromRGB(255, 255, 255)
	winStroke.Thickness = 3
	winStroke.Transparency = 0.2
	winStroke.Parent = win

	-- aparece com um "poft" (cresce e fica visível)
	win.Size = UDim2.new(0, 200, 0, 80)
	local fadeIn = TweenService:Create(
		win,
		TweenInfo.new(0.45, Enum.EasingStyle.Back, Enum.EasingDirection.Out),
		{
			BackgroundTransparency = 0.05,
			TextTransparency = 0,
			Size = UDim2.new(0, 560, 0, 200),
		}
	)
	fadeIn:Play()

	-- some depois de 5 segundos e libera o cronômetro de novo
	task.delay(5, function()
		local fadeOut = TweenService:Create(win, TweenInfo.new(0.6), {
			BackgroundTransparency = 1,
			TextTransparency = 1,
		})
		fadeOut:Play()
		fadeOut.Completed:Connect(function()
			win:Destroy()
		end)
		won = false
	end)
end

local finishedEvent = ReplicatedStorage:WaitForChild("ObbyFinished") :: RemoteEvent
finishedEvent.OnClientEvent:Connect(showWinScreen)
