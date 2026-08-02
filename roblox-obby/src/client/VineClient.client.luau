--!strict
-- VineClient.client.luau  ->  vai em StarterPlayer > StarterPlayerScripts
-- =========================================================================
-- Duas coisas:
--   1. Quando você aperta ESPAÇO (pulo), avisa o servidor pra SOLTAR o cipó.
--   2. Mostra uma dica na tela explicando os controles.
-- =========================================================================

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local player = Players.LocalPlayer
local releaseEvent = ReplicatedStorage:WaitForChild("VineRelease") :: RemoteEvent

-- avisa o servidor sempre que o jogador tenta pular (espaço / botão de pulo)
UserInputService.JumpRequest:Connect(function()
	releaseEvent:FireServer()
end)

-- ---- dica na tela ----
local gui = Instance.new("ScreenGui")
gui.Name = "VineHintGui"
gui.ResetOnSpawn = false
gui.Parent = player:WaitForChild("PlayerGui")

local hint = Instance.new("TextLabel")
hint.AnchorPoint = Vector2.new(0.5, 1)
hint.Position = UDim2.new(0.5, 0, 0.95, 0)
hint.Size = UDim2.new(0, 440, 0, 44)
hint.BackgroundColor3 = Color3.fromRGB(20, 30, 18)
hint.BackgroundTransparency = 0.25
hint.TextColor3 = Color3.fromRGB(235, 255, 230)
hint.Font = Enum.Font.GothamMedium
hint.TextScaled = true
hint.Text = "🌿 Encoste no cipó para se pendurar  •  Espaço para soltar"
hint.Parent = gui

local corner = Instance.new("UICorner")
corner.CornerRadius = UDim.new(0, 12)
corner.Parent = hint

local pad = Instance.new("UIPadding")
pad.PaddingLeft = UDim.new(0, 14)
pad.PaddingRight = UDim.new(0, 14)
pad.Parent = hint
