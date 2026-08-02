--!strict
-- VineControl.client.luau  ->  vai em StarterPlayer > StarterPlayerScripts
-- =========================================================================
-- Controla o BALANÇO do cipó enquanto você está pendurado 🌿
--   * Parado (sem tecla) -> o cipó fica PARADO, só pendurado
--   * W -> balança PRA FRENTE (na direção pra onde a câmera olha)
--   * S -> balança PRA TRÁS
--
-- Segure W pra ganhar embalo e subir alto; no auge, aperte ESPAÇO
-- (o VineClient cuida de soltar) pra ser lançado até a próxima ilha. 🚀
--
-- Como você é o dono do cipó na rede (o servidor te deu o controle físico),
-- mexer na velocidade do cipó aqui no cliente fica bem suave.
-- =========================================================================

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")
local RunService = game:GetService("RunService")

local player = Players.LocalPlayer
local camera = workspace.CurrentCamera

-- afinação do balanço:
local ACCEL = 75 -- quão rápido ganha força ao segurar W/S
local MAX_SWING = 55 -- velocidade máxima do balanço (mais alto = balança mais forte)

local forwardHeld = false -- W
local backHeld = false -- S

UserInputService.InputBegan:Connect(function(input)
	if input.KeyCode == Enum.KeyCode.W then
		forwardHeld = true
	elseif input.KeyCode == Enum.KeyCode.S then
		backHeld = true
	end
end)
UserInputService.InputEnded:Connect(function(input)
	if input.KeyCode == Enum.KeyCode.W then
		forwardHeld = false
	elseif input.KeyCode == Enum.KeyCode.S then
		backHeld = false
	end
end)

RunService.Heartbeat:Connect(function(dt)
	local character = player.Character
	if not character or not character:GetAttribute("OnVine") then
		return
	end
	local vineRef = character:FindFirstChild("CurrentVine") :: ObjectValue?
	local vine = (vineRef and vineRef.Value) :: BasePart?
	if not vine then
		return
	end

	-- "pra frente" = direção pra onde a câmera olha, achatada no plano do chão
	local look = camera.CFrame.LookVector
	local flat = Vector3.new(look.X, 0, look.Z)
	if flat.Magnitude < 0.05 then
		return
	end
	flat = flat.Unit

	local dir = Vector3.zero
	if forwardHeld then
		dir += flat
	end
	if backHeld then
		dir -= flat
	end
	-- nenhuma tecla -> não empurra nada: o cipó segue só a física
	-- (se estava parado, continua parado; se balançava, continua o pêndulo)
	if dir.Magnitude < 0.05 then
		return
	end
	dir = dir.Unit

	-- acelera o balanço na direção escolhida, preservando a subida/descida
	-- natural do pêndulo (só mexemos na parte horizontal da velocidade).
	local v = vine.AssemblyLinearVelocity
	local horiz = Vector3.new(v.X, 0, v.Z) + dir * ACCEL * dt
	if horiz.Magnitude > MAX_SWING then
		horiz = horiz.Unit * MAX_SWING
	end
	vine.AssemblyLinearVelocity = Vector3.new(horiz.X, v.Y, horiz.Z)
end)
