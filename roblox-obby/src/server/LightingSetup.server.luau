--!strict
-- LightingSetup.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Dá aquele visual "uau" de jogo profissional: céu bonito, atmosfera,
-- brilho (bloom) e correção de cor. Sem isso o jogo fica com cara de
-- Baseplate cru; com isso fica com cara de jogo de verdade. ✨
-- =========================================================================

local Lighting = game:GetService("Lighting")

-- horário/tom geral
Lighting.ClockTime = 15 -- fim de tarde suave
Lighting.Brightness = 2.5
Lighting.ExposureCompensation = 0.15
Lighting.Ambient = Color3.fromRGB(70, 70, 90)
Lighting.OutdoorAmbient = Color3.fromRGB(120, 130, 160)
Lighting.EnvironmentDiffuseScale = 1
Lighting.EnvironmentSpecularScale = 1
Lighting.GlobalShadows = true

-- limpa efeitos antigos pra não duplicar se rodar de novo
for _, obj in Lighting:GetChildren() do
	if obj:IsA("PostEffect") or obj:IsA("Atmosphere") or obj:IsA("Sky") then
		obj:Destroy()
	end
end

-- atmosfera (dá profundidade e neblina bonita ao longe)
local atmosphere = Instance.new("Atmosphere")
atmosphere.Density = 0.32
atmosphere.Offset = 0.1
atmosphere.Color = Color3.fromRGB(199, 200, 219)
atmosphere.Decay = Color3.fromRGB(106, 112, 152)
atmosphere.Glare = 0.2
atmosphere.Haze = 1.4
atmosphere.Parent = Lighting

-- brilho nas partes Neon (moedas, lava, chegada brilham lindo)
local bloom = Instance.new("BloomEffect")
bloom.Intensity = 0.8
bloom.Size = 24
bloom.Threshold = 0.9
bloom.Parent = Lighting

-- correção de cor: deixa as cores mais vivas e "gostosas"
local cc = Instance.new("ColorCorrectionEffect")
cc.Brightness = 0.02
cc.Contrast = 0.12
cc.Saturation = 0.18
cc.TintColor = Color3.fromRGB(255, 250, 245)
cc.Parent = Lighting

-- leve profundidade de campo pra dar aquele charme cinematográfico
local dof = Instance.new("DepthOfFieldEffect")
dof.FarIntensity = 0.15
dof.FocusDistance = 30
dof.InFocusRadius = 60
dof.NearIntensity = 0
dof.Parent = Lighting
