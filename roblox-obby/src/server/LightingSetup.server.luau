--!strict
-- LightingSetup.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Dá aquele visual "uau" de jogo profissional: céu bonito, atmosfera,
-- brilho (bloom) e correção de cor. Sem isso o jogo fica com cara de
-- Baseplate cru; com isso fica com cara de jogo de verdade. ✨
-- =========================================================================

local Lighting = game:GetService("Lighting")

-- horário/tom geral — clima de FLORESTA 🌿
Lighting.ClockTime = 13 -- sol alto, luz entrando entre as folhas
Lighting.Brightness = 2
Lighting.ExposureCompensation = 0
Lighting.Ambient = Color3.fromRGB(70, 85, 70) -- sombra esverdeada
Lighting.OutdoorAmbient = Color3.fromRGB(120, 140, 110) -- luz com tom de mata
Lighting.EnvironmentDiffuseScale = 0.6
Lighting.EnvironmentSpecularScale = 0.6
Lighting.GlobalShadows = true

-- limpa efeitos antigos pra não duplicar se rodar de novo
for _, obj in Lighting:GetChildren() do
	if obj:IsA("PostEffect") or obj:IsA("Atmosphere") or obj:IsA("Sky") then
		obj:Destroy()
	end
end

-- atmosfera — neblina esverdeada de floresta ao fundo 🌫️
local atmosphere = Instance.new("Atmosphere")
atmosphere.Density = 0.36
atmosphere.Offset = 0.1
atmosphere.Color = Color3.fromRGB(180, 205, 175)
atmosphere.Decay = Color3.fromRGB(90, 120, 80)
atmosphere.Glare = 0
atmosphere.Haze = 1.6
atmosphere.Parent = Lighting

-- brilho SÓ nas partes bem neon (moedas, lava, chegada), sem lavar a tela
local bloom = Instance.new("BloomEffect")
bloom.Intensity = 0.35 -- (era 0.8 — brilho controlado agora)
bloom.Size = 20
bloom.Threshold = 1.1 -- só o que é MUITO brilhante ganha o glow
bloom.Parent = Lighting

-- correção de cor: deixa as cores mais vivas sem exagerar
local cc = Instance.new("ColorCorrectionEffect")
cc.Brightness = 0
cc.Contrast = 0.08
cc.Saturation = 0.12
cc.TintColor = Color3.fromRGB(255, 252, 248)
cc.Parent = Lighting

-- leve profundidade de campo pra dar aquele charme cinematográfico
local dof = Instance.new("DepthOfFieldEffect")
dof.FarIntensity = 0.15
dof.FocusDistance = 30
dof.InFocusRadius = 60
dof.NearIntensity = 0
dof.Parent = Lighting
