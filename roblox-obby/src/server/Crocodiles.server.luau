--!strict
-- Crocodiles.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Os JACARÉS do lago 🐊
--   * Nadam sozinhos pela água, com a boca abrindo e fechando (mordida)
--   * Se você CAIR na água, o jacaré mais perto corre até você e te come
--     (você "morre" e renasce na ilha de partida)
--
-- Tudo montado por código a partir de peças — sem precisar de mesh/asset.
-- =========================================================================

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")

local SCALE_COLOR = Color3.fromRGB(52, 84, 46) -- verde escuro de jacaré
local SCALE_DARK = Color3.fromRGB(38, 62, 34)
local BELLY = Color3.fromRGB(120, 130, 90)

-- Limites da água (a zona de perigo). Fora daqui é ilha/terra firme.
local WATER = {
	xMin = -70,
	xMax = 70,
	zMin = -34,
	zMax = 34,
	surfaceY = 0,
}
local LETHAL_Y = 2.5 -- abaixo disso, dentro da água = você tá no lago

local crocs: { Model } = {}

-- ---------------------------------------------------------------------
-- Constrói UM jacaré (só peças). Retorna o model e a junta da mandíbula.
-- ---------------------------------------------------------------------
local function buildCroc(name: string): (Model, BasePart, Motor6D)
	local model = Instance.new("Model")
	model.Name = name

	local function part(size: Vector3, color: Color3, shape: Enum.PartType?): BasePart
		local p = Instance.new("Part")
		p.Size = size
		p.Color = color
		p.Material = Enum.Material.SmoothPlastic
		p.Shape = shape or Enum.PartType.Block
		p.Anchored = false
		p.CanCollide = false
		p.TopSurface = Enum.SurfaceType.Smooth
		p.BottomSurface = Enum.SurfaceType.Smooth
		p.Parent = model
		return p
	end

	-- corpo (PrimaryPart) — flutua na superfície
	local body = part(Vector3.new(3, 1.4, 9), SCALE_COLOR)
	model.PrimaryPart = body

	-- barriga mais clara
	local belly = part(Vector3.new(2.6, 0.6, 8.4), BELLY)
	local bellyWeld = Instance.new("Weld")
	bellyWeld.Part0 = body
	bellyWeld.Part1 = belly
	bellyWeld.C1 = CFrame.new(0, 0.6, 0)
	bellyWeld.Parent = body

	-- escamas nas costas (fileira de cubinhos)
	for i = -3, 3 do
		local scale = part(Vector3.new(1, 0.7, 0.7), SCALE_DARK)
		scale.Shape = Enum.PartType.Block
		local w = Instance.new("Weld")
		w.Part0 = body
		w.Part1 = scale
		w.C1 = CFrame.new(0, -0.8, i * 1.1) * CFrame.Angles(0, math.rad(45), 0)
		w.Parent = body
	end

	-- cabeça (topo da mandíbula) fixa no corpo, na frente
	local head = part(Vector3.new(2.6, 1, 4), SCALE_COLOR)
	local headWeld = Instance.new("Weld")
	headWeld.Part0 = body
	headWeld.Part1 = head
	headWeld.C1 = CFrame.new(0, -0.1, 6) -- frente do corpo
	headWeld.Parent = body

	-- olhos
	for _, sx in { -0.8, 0.8 } do
		local eye = part(Vector3.new(0.6, 0.6, 0.6), Color3.fromRGB(230, 220, 60), Enum.PartType.Ball)
		local w = Instance.new("Weld")
		w.Part0 = head
		w.Part1 = eye
		w.C1 = CFrame.new(sx, -0.6, -1.2)
		w.Parent = head
		local pupil = part(Vector3.new(0.28, 0.36, 0.36), Color3.fromRGB(15, 15, 15), Enum.PartType.Ball)
		local pw = Instance.new("Weld")
		pw.Part0 = eye
		pw.Part1 = pupil
		pw.C1 = CFrame.new(0, 0, 0.28)
		pw.Parent = eye
	end

	-- MANDÍBULA de baixo — presa por Motor6D pra poder abrir/fechar
	local jaw = part(Vector3.new(2.4, 0.7, 3.6), SCALE_DARK)
	local jawMotor = Instance.new("Motor6D")
	jawMotor.Name = "JawMotor"
	jawMotor.Part0 = head
	-- pivô na parte de trás da mandíbula (perto da "dobradiça")
	jawMotor.C0 = CFrame.new(0, -0.5, -1.6)
	jawMotor.C1 = CFrame.new(0, 0, -1.6)
	jawMotor.Part1 = jaw
	jawMotor.Parent = head

	-- dentes (na mandíbula de cima e de baixo)
	local function teeth(parentPart: BasePart, yOff: number, dir: number)
		for i = -2, 2 do
			local tooth = part(Vector3.new(0.28, 0.7, 0.28), Color3.fromRGB(245, 245, 235))
			local w = Instance.new("Weld")
			w.Part0 = parentPart
			w.Part1 = tooth
			w.C1 = CFrame.new(i * 0.55, yOff, -0.4 + math.abs(i) * 0.3) * CFrame.Angles(math.rad(dir), 0, 0)
			w.Parent = parentPart
		end
	end
	teeth(head, 0.4, 0)
	teeth(jaw, -0.35, 180)

	-- patas
	for _, off in { Vector3.new(-1.7, -0.6, 3), Vector3.new(1.7, -0.6, 3), Vector3.new(-1.7, -0.6, -3), Vector3.new(1.7, -0.6, -3) } do
		local leg = part(Vector3.new(0.8, 1, 1.6), SCALE_DARK)
		local w = Instance.new("Weld")
		w.Part0 = body
		w.Part1 = leg
		w.C1 = CFrame.new(off) * CFrame.Angles(0, math.rad(off.X > 0 and 25 or -25), 0)
		w.Parent = body
	end

	-- cauda (dois segmentos que afinam)
	local tail1 = part(Vector3.new(2, 1, 3.5), SCALE_COLOR)
	local tw1 = Instance.new("Weld")
	tw1.Part0 = body
	tw1.Part1 = tail1
	tw1.C1 = CFrame.new(0, 0, -6)
	tw1.Parent = body
	local tail2 = part(Vector3.new(1.2, 0.7, 3), SCALE_DARK)
	local tw2 = Instance.new("Weld")
	tw2.Part0 = tail1
	tw2.Part1 = tail2
	tw2.C1 = CFrame.new(0, 0, -3)
	tw2.Parent = tail1

	return model, body, jawMotor
end

-- ---------------------------------------------------------------------
-- Cria N jacarés espalhados pelo lago
-- ---------------------------------------------------------------------
type Croc = {
	model: Model,
	body: BasePart,
	jaw: Motor6D,
	target: Vector3,
	chasing: Player?,
	speed: number,
	phase: number,
}

local crocData: { Croc } = {}

local function randomWaterPoint(): Vector3
	return Vector3.new(
		math.random(WATER.xMin + 10, WATER.xMax - 10),
		WATER.surfaceY + 0.3,
		math.random(WATER.zMin + 6, WATER.zMax - 6)
	)
end

local NUM_CROCS = 5
for i = 1, NUM_CROCS do
	local model, body, jaw = buildCroc("Croc" .. i)
	local start = randomWaterPoint()
	body.CFrame = CFrame.new(start, start + Vector3.new(math.random(-10, 10), 0, math.random(-10, 10)))
	model.Parent = workspace

	-- deixa o corpo flutuar em pé, sem tombar (junta rígida já mantém tudo unido;
	-- movemos o corpo direto por CFrame no loop, então mantemos Anchored = false
	-- mas sem gravidade atrapalhando: usamos AlignPosition/Orientation leves).
	body.Anchored = true -- movido por código; simples e estável na superfície

	table.insert(crocs, model)
	table.insert(crocData, {
		model = model,
		body = body,
		jaw = jaw,
		target = randomWaterPoint(),
		chasing = nil,
		speed = 8 + math.random() * 4,
		phase = math.random() * 10,
	})
end

-- ---------------------------------------------------------------------
-- Está dentro da água (zona letal)?
-- ---------------------------------------------------------------------
local function inWater(pos: Vector3): boolean
	return pos.Y < LETHAL_Y
		and pos.X > WATER.xMin
		and pos.X < WATER.xMax
		and pos.Z > WATER.zMin
		and pos.Z < WATER.zMax
end

-- efeito de mordida + morte
local eaten: { [Player]: boolean } = {}
local function eat(player: Player, hrp: BasePart)
	if eaten[player] then
		return
	end
	eaten[player] = true

	-- respingo/mordida
	local splash = Instance.new("Part")
	splash.Anchored = true
	splash.CanCollide = false
	splash.Shape = Enum.PartType.Ball
	splash.Size = Vector3.new(6, 6, 6)
	splash.Position = hrp.Position
	splash.Color = Color3.fromRGB(120, 150, 120)
	splash.Material = Enum.Material.Water
	splash.Transparency = 0.3
	splash.Parent = workspace
	task.delay(0.4, function()
		splash:Destroy()
	end)

	local character = hrp.Parent :: Model?
	local humanoid = character and character:FindFirstChildOfClass("Humanoid")
	if humanoid then
		humanoid.Health = 0
	end
	task.delay(2, function()
		eaten[player] = nil
	end)
end

-- ---------------------------------------------------------------------
-- Loop principal: nadar, morder e caçar
-- ---------------------------------------------------------------------
RunService.Heartbeat:Connect(function(dt)
	-- 1) quem tá na água? marca pro jacaré mais perto caçar
	for _, player in Players:GetPlayers() do
		local character = player.Character
		local hrp = character and character:FindFirstChild("HumanoidRootPart") :: BasePart?
		local onVine = character and character:GetAttribute("OnVine")
		if hrp and not onVine and inWater(hrp.Position) then
			-- acha o jacaré mais próximo e manda ele atacar
			local best: Croc? = nil
			local bestDist = math.huge
			for _, c in crocData do
				local d = (c.body.Position - hrp.Position).Magnitude
				if d < bestDist then
					bestDist = d
					best = c
				end
			end
			if best then
				best.chasing = player
			end
			-- se já tá bem raso/dentro, morde na hora quando o jacaré chega perto
			for _, c in crocData do
				if (c.body.Position - hrp.Position).Magnitude < 6 then
					eat(player, hrp)
				end
			end
		end
	end

	-- 2) mover cada jacaré
	for _, c in crocData do
		c.phase += dt

		local goal: Vector3
		local chasingHrp: BasePart? = nil
		if c.chasing then
			local char = c.chasing.Character
			chasingHrp = char and char:FindFirstChild("HumanoidRootPart") :: BasePart?
			local onVine = char and char:GetAttribute("OnVine")
			if chasingHrp and not onVine and inWater(chasingHrp.Position) then
				goal = Vector3.new(chasingHrp.Position.X, WATER.surfaceY + 0.3, chasingHrp.Position.Z)
			else
				c.chasing = nil -- alvo saiu da água / soltou
			end
		end

		if not c.chasing then
			-- patrulha: vagueia até o alvo, escolhe outro ao chegar
			goal = c.target
			if (Vector3.new(c.body.Position.X, 0, c.body.Position.Z) - Vector3.new(c.target.X, 0, c.target.Z)).Magnitude < 4 then
				c.target = randomWaterPoint()
			end
		end

		local pos = c.body.Position
		local flatGoal = Vector3.new(goal.X, WATER.surfaceY + 0.3, goal.Z)
		local dir = flatGoal - pos
		local dist = dir.Magnitude
		local speed = c.chasing and (c.speed + 6) or c.speed
		local step = math.min(dist, speed * dt)
		local newPos = dist > 0.1 and (pos + dir.Unit * step) or pos

		-- balanço suave da cauda: um leve rolar + subir/descer na "água"
		local bob = math.sin(c.phase * 3) * 0.12
		newPos = Vector3.new(newPos.X, WATER.surfaceY + 0.3 + bob, newPos.Z)

		-- olha pra direção do movimento
		local lookAt = dist > 0.1 and (newPos + dir.Unit) or (newPos + c.body.CFrame.LookVector)
		local roll = math.sin(c.phase * 4) * 0.08 -- rebolado natatório
		c.body.CFrame = CFrame.new(newPos, lookAt) * CFrame.Angles(0, 0, roll)

		-- boca: fecha, mas escancara quando tá caçando/chegando perto
		local openTarget: number
		if c.chasing and chasingHrp and (c.body.Position - chasingHrp.Position).Magnitude < 14 then
			openTarget = math.rad(35) -- BOCA ABERTA no ataque 🐊
		else
			openTarget = (math.sin(c.phase * 1.5) * 0.5 + 0.5) * math.rad(14) -- mastiga de leve
		end
		-- abre/fecha girando a mandíbula no eixo X (na dobradiça de trás)
		c.jaw.C0 = CFrame.new(0, -0.5, -1.6) * CFrame.Angles(-openTarget, 0, 0)
	end
end)

-- limpeza
Players.PlayerRemoving:Connect(function(player)
	eaten[player] = nil
	for _, c in crocData do
		if c.chasing == player then
			c.chasing = nil
		end
	end
end)

print("[Jacarés] 🐊 " .. NUM_CROCS .. " jacarés soltos no lago. Não caia na água!")
