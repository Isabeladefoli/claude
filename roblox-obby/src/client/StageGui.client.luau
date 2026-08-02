--!strict
-- StageGui.client.luau  ->  vai em StarterPlayer > StarterPlayerScripts
-- =========================================================================
-- Monta na tela do jogador:
--   * um contador "Estágio: N" no topo
--   * uma tela grande de "VOCÊ VENCEU! 🏆" quando chega no Finish
--
-- Tudo criado por código, então você não precisa desenhar GUI no Studio.
-- Depois, se quiser deixar bonito, é só mexer nas cores/posições aqui.
-- =========================================================================

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local TweenService = game:GetService("TweenService")

local player = Players.LocalPlayer

-- ---- ScreenGui base ----
local gui = Instance.new("ScreenGui")
gui.Name = "ObbyGui"
gui.ResetOnSpawn = false
gui.ZIndexBehavior = Enum.ZIndexBehavior.Sibling
gui.Parent = player:WaitForChild("PlayerGui")

-- ---- Contador de estágio ----
local stageLabel = Instance.new("TextLabel")
stageLabel.Name = "StageLabel"
stageLabel.AnchorPoint = Vector2.new(0.5, 0)
stageLabel.Position = UDim2.new(0.5, 0, 0.03, 0)
stageLabel.Size = UDim2.new(0, 220, 0, 48)
stageLabel.BackgroundColor3 = Color3.fromRGB(20, 20, 30)
stageLabel.BackgroundTransparency = 0.25
stageLabel.TextColor3 = Color3.fromRGB(255, 255, 255)
stageLabel.Font = Enum.Font.GothamBold
stageLabel.TextScaled = true
stageLabel.Text = "Estágio: 0"
stageLabel.Parent = gui

local corner = Instance.new("UICorner")
corner.CornerRadius = UDim.new(0, 12)
corner.Parent = stageLabel

-- Segue o valor "Stage" do leaderstats e atualiza o texto
local function watchStage()
	local leaderstats = player:WaitForChild("leaderstats")
	local stage = leaderstats:WaitForChild("Stage") :: IntValue

	local function update()
		stageLabel.Text = "Estágio: " .. tostring(stage.Value)
	end
	update()
	stage.Changed:Connect(update)
end
task.spawn(watchStage)

-- ---- Tela de vitória ----
local function showWinScreen()
	local win = Instance.new("TextLabel")
	win.AnchorPoint = Vector2.new(0.5, 0.5)
	win.Position = UDim2.new(0.5, 0, 0.5, 0)
	win.Size = UDim2.new(0, 500, 0, 140)
	win.BackgroundColor3 = Color3.fromRGB(30, 200, 120)
	win.TextColor3 = Color3.fromRGB(255, 255, 255)
	win.Font = Enum.Font.GothamBlack
	win.TextScaled = true
	win.Text = "VOCÊ VENCEU! 🏆"
	win.BackgroundTransparency = 1
	win.TextTransparency = 1
	win.Parent = gui

	local winCorner = Instance.new("UICorner")
	winCorner.CornerRadius = UDim.new(0, 20)
	winCorner.Parent = win

	-- aparece suave
	local fadeIn = TweenService:Create(win, TweenInfo.new(0.4), {
		BackgroundTransparency = 0.1,
		TextTransparency = 0,
	})
	fadeIn:Play()

	-- some depois de 4 segundos
	task.delay(4, function()
		local fadeOut = TweenService:Create(win, TweenInfo.new(0.6), {
			BackgroundTransparency = 1,
			TextTransparency = 1,
		})
		fadeOut:Play()
		fadeOut.Completed:Connect(function()
			win:Destroy()
		end)
	end)
end

local finishedEvent = ReplicatedStorage:WaitForChild("ObbyFinished") :: RemoteEvent
finishedEvent.OnClientEvent:Connect(showWinScreen)
