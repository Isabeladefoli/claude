--!strict
-- LightingSetup.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Dá aquele visual "uau" de jogo profissional: céu bonito, atmosfera,
-- brilho (bloom) e correção de cor. Sem isso o jogo fica com cara de
-- Baseplate cru; com isso fica com cara de jogo de verdade. ✨
-- =========================================================================

local Lighting = game:GetService("Lighting")

-- horário/tom geral
Lighting.ClockTime = 14 -- tarde
Lighting.Brightness = 2 -- (era 2.5 e estourava tudo de branco)
Lighting.ExposureCompensation = 0 -- sem "super brilho" na câmera
Lighting.Ambient = Color3.fromRGB(80, 80, 95)
Lighting.OutdoorAmbient = Color3.fromRGB(140, 145, 165)
Lighting.EnvironmentDiffuseScale = 0.6
Lighting.EnvironmentSpecularScale = 0.6
Lighting.GlobalShadows = true

-- limpa efeitos antigos pra não duplicar se rodar de novo
for _, obj in Lighting:GetChildren() do
	if obj:IsA("PostEffect") or obj:IsA("Atmosphere") or obj:IsA("Sky") then
		obj:Destroy()
	end
end

-- atmosfera (dá profundidade e neblina bonita ao longe)
local atmosphere = Instance.new("Atmosphere")
atmosphere.Density = 0.28
atmosphere.Offset = 0.1
atmosphere.Color = Color3.fromRGB(199, 200, 219)
atmosphere.Decay = Color3.fromRGB(106, 112, 152)
atmosphere.Glare = 0 -- (era o que mais "estourava" o céu de branco)
atmosphere.Haze = 0.8
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
