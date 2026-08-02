--!strict
-- ArmPose.client.luau  ->  vai em StarterPlayer > StarterPlayerScripts
-- =========================================================================
-- Faz o BRAÇO do personagem levantar como se estivesse REALMENTE segurando
-- o cipó. 🙌🌿
--
-- Como funciona: o servidor liga o atributo "OnVine" no personagem quando
-- você agarra o cipó. Aqui a gente escuta esse atributo e, enquanto estiver
-- ligado, gira as juntas dos ombros (os dois braços) pra cima a cada frame
-- (sobrepondo a animação padrão de "parado"). Ao soltar, volta ao normal.
--
-- Como você é o dono do seu personagem na rede, esse ajuste replica pro
-- servidor e os OUTROS jogadores também te veem pendurado. 👀
-- =========================================================================

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")

local player = Players.LocalPlayer

-- quanto o braço sobe. Se quiser afinar a pose, mexa nestes ângulos.
local RAISE = math.rad(150)

type Joint = { motor: Motor6D, original: CFrame, pose: CFrame }

local function setupCharacter(character: Model)
	local humanoid = character:WaitForChild("Humanoid") :: Humanoid

	-- acha as juntas dos ombros procurando pelo Part1 (funciona R6 e R15):
	--   R6:  Part1 = "Right Arm" / "Left Arm"        (Motor6D no Torso)
	--   R15: Part1 = "RightUpperArm" / "LeftUpperArm" (Motor6D no braço)
	local function findShoulder(armNames: { string }): Motor6D?
		for _, d in character:GetDescendants() do
			if d:IsA("Motor6D") and d.Part1 then
				for _, n in armNames do
					if d.Part1.Name == n then
						return d
					end
				end
			end
		end
		return nil
	end

	local rightMotor = findShoulder({ "Right Arm", "RightUpperArm" })
	local leftMotor = findShoulder({ "Left Arm", "LeftUpperArm" })

	local joints: { Joint } = {}
	if rightMotor then
		table.insert(joints, {
			motor = rightMotor,
			original = rightMotor.C0,
			-- braço direito sobe girando pra "trás/cima" no eixo Z local
			pose = rightMotor.C0 * CFrame.Angles(0, 0, -RAISE),
		})
	end
	if leftMotor then
		table.insert(joints, {
			motor = leftMotor,
			original = leftMotor.C0,
			pose = leftMotor.C0 * CFrame.Angles(0, 0, RAISE),
		})
	end

	local connection: RBXScriptConnection? = nil

	local function stopPose()
		if connection then
			connection:Disconnect()
			connection = nil
		end
		for _, j in joints do
			j.motor.C0 = j.original
		end
	end

	local function startPose()
		if connection then
			return
		end
		-- a cada frame, sobrepõe a animação padrão travando os ombros na pose
		connection = RunService.RenderStepped:Connect(function()
			for _, j in joints do
				j.motor.C0 = j.pose
				j.motor.Transform = CFrame.identity -- congela a sacudida da animação
			end
		end)
	end

	character:GetAttributeChangedSignal("OnVine"):Connect(function()
		if character:GetAttribute("OnVine") then
			startPose()
		else
			stopPose()
		end
	end)

	-- caso já esteja pendurado quando o script liga
	if character:GetAttribute("OnVine") then
		startPose()
	end

	humanoid.Died:Connect(stopPose)
end

if player.Character then
	setupCharacter(player.Character)
end
player.CharacterAdded:Connect(setupCharacter)
