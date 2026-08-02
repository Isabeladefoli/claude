--!strict
-- World.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Monta a cena estilo TARZAN 🐊🌿:
--   * Uma ilha de partida (onde você nasce) e uma ilha de chegada (o troféu)
--   * Um LAGO de água de verdade (terrain) no meio, cheio de jacarés
--   * DUAS árvores, cada uma com o SEU cipó pendurado sobre a água
--
-- O objetivo: pular no cipó 1, balançar, se soltar no embalo, agarrar o
-- cipó 2 no ar e pousar na ilha de chegada — SEM cair na água. 🏆
--
-- A lógica de agarrar/soltar fica no VineGrab.server.luau.
-- Os jacarés e a "morte na água" ficam no Crocodiles.server.luau.
-- A pose do braço segurando o cipó fica no ArmPose.client.luau.
-- =========================================================================

local CollectionService = game:GetService("CollectionService")
local Terrain = workspace.Terrain

-- ---- cores naturais ----
local BARK = Color3.fromRGB(92, 62, 41)
local BARK_DARK = Color3.fromRGB(72, 48, 32)
local LEAF_A = Color3.fromRGB(60, 110, 45)
local LEAF_B = Color3.fromRGB(84, 142, 58)
local LEAF_C = Color3.fromRGB(104, 168, 74)
local VINE_COLOR = Color3.fromRGB(74, 120, 52)
local SAND = Color3.fromRGB(206, 188, 132)
local GRASS = Color3.fromRGB(86, 138, 62)

local world = Instance.new("Folder")
world.Name = "World"
world.Parent = workspace

-- ======================================================================
--  Helpers
-- ======================================================================
local function make(class: string, props: { [string]: any }): Instance
	local inst = Instance.new(class)
	for k, v in props do
		(inst :: any)[k] = v
	end
	inst.Parent = props.Parent or world
	return inst
end

local function cylinder(size: Vector3, cframe: CFrame, color: Color3, material: Enum.Material, parent: Instance?): BasePart
	return make("Part", {
		Shape = Enum.PartType.Cylinder,
		Size = size,
		CFrame = cframe,
		Color = color,
		Material = material,
		Anchored = true,
		TopSurface = Enum.SurfaceType.Smooth,
		BottomSurface = Enum.SurfaceType.Smooth,
		Parent = parent,
	}) :: BasePart
end

local function blob(size: Vector3, pos: Vector3, color: Color3, parent: Instance?): BasePart
	return make("Part", {
		Shape = Enum.PartType.Ball,
		Size = size,
		Position = pos,
		Color = color,
		Material = Enum.Material.Grass,
		Anchored = true,
		CanCollide = false,
		Parent = parent,
	}) :: BasePart
end

-- ilha redonda de grama sentada logo acima da água
local function island(center: Vector3, radius: number, name: string): BasePart
	local top = make("Part", {
		Name = name,
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(3, radius * 2, radius * 2),
		CFrame = CFrame.new(center) * CFrame.Angles(0, 0, math.rad(90)),
		Color = GRASS,
		Material = Enum.Material.Grass,
		Anchored = true,
	}) :: BasePart
	-- praia de areia em volta, um degrau abaixo
	make("Part", {
		Name = name .. "_Beach",
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(2.4, radius * 2 + 6, radius * 2 + 6),
		CFrame = CFrame.new(center - Vector3.new(0, 1, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Color = SAND,
		Material = Enum.Material.Sand,
		Anchored = true,
	})
	return top
end

-- ======================================================================
--  ÁGUA DE VERDADE (terrain) — o lago dos jacarés 🐊
-- ======================================================================
-- superfície da água em y = 0. As ilhas ficam com o topo em y = +1.5.
Terrain:Clear()
Terrain:FillBlock(
	CFrame.new(0, -6, 0),
	Vector3.new(220, 12, 220),
	Enum.Material.Water
)

-- ======================================================================
--  ILHAS  (partida, chegada e as duas margens das árvores)
-- ======================================================================
-- topo das ilhas em y = 1.5 (acima da água)
local START_ISLAND = Vector3.new(0, 0, 46)
local GOAL_ISLAND = Vector3.new(0, 0, -46)
local TREE_A_BASE = Vector3.new(-16, 0, 20) -- margem esquerda
local TREE_B_BASE = Vector3.new(16, 0, -14) -- margem direita

island(START_ISLAND, 16, "StartIsland")
island(GOAL_ISLAND, 16, "GoalIsland")
island(TREE_A_BASE, 9, "TreeIslandA")
island(TREE_B_BASE, 9, "TreeIslandB")

-- ponto de nascimento na ilha de partida
make("SpawnLocation", {
	Size = Vector3.new(8, 1, 8),
	Position = START_ISLAND + Vector3.new(0, 2, 0),
	Color = Color3.fromRGB(110, 165, 85),
	Material = Enum.Material.Grass,
	Anchored = true,
})

-- ======================================================================
--  ÁRVORE 🌳 (função reaproveitável)
-- ======================================================================
-- Desenha uma árvore na base dada e estica um galho forte até `anchorPos`,
-- que é o ponto de onde o cipó vai pendurar.
local function makeTree(base: Vector3, anchorPos: Vector3)
	local tree = make("Folder", { Name = "Tree", Parent = world }) :: Folder

	-- tronco: cilindros empilhados que vão afinando
	local trunkSections = {
		{ y = 3, r = 4.0 },
		{ y = 9, r = 3.4 },
		{ y = 15, r = 2.8 },
		{ y = 21, r = 2.3 },
	}
	for _, s in trunkSections do
		cylinder(
			Vector3.new(7, s.r * 2, s.r * 2),
			CFrame.new(base + Vector3.new(0, s.y, 0)) * CFrame.Angles(0, 0, math.rad(90)),
			s.y < 12 and BARK or BARK_DARK,
			Enum.Material.Wood,
			tree
		)
	end

	-- raízes na base
	for i = 0, 5 do
		local ang = math.rad(i * 60)
		local dir = Vector3.new(math.cos(ang), 0, math.sin(ang))
		local rootPos = base + dir * 4 + Vector3.new(0, 0.6, 0)
		cylinder(
			Vector3.new(5, 1.6, 1.6),
			CFrame.new(rootPos, rootPos + dir + Vector3.new(0, -0.4, 0)) * CFrame.Angles(0, math.rad(90), 0),
			BARK_DARK,
			Enum.Material.Wood,
			tree
		)
	end

	-- galhos decorativos no alto
	local branches = {
		{ dir = Vector3.new(1, 0.5, 0.2), len = 11 },
		{ dir = Vector3.new(-0.8, 0.6, -0.3), len = 10 },
		{ dir = Vector3.new(0.1, 0.7, 1), len = 9 },
		{ dir = Vector3.new(-0.2, 0.6, -1), len = 9 },
	}
	for _, b in branches do
		local dir = b.dir.Unit
		local start = base + Vector3.new(0, 21, 0)
		local finish = start + dir * b.len
		local mid = (start + finish) / 2
		cylinder(
			Vector3.new(b.len, 1.8, 1.8),
			CFrame.new(mid, finish) * CFrame.Angles(0, math.rad(90), 0),
			BARK,
			Enum.Material.Wood,
			tree
		)
	end

	-- galho FORTE que vai até o ponto do cipó (por cima da água)
	local branchStart = base + Vector3.new(0, 23, 0)
	cylinder(
		Vector3.new((branchStart - anchorPos).Magnitude + 1, 2.2, 2.2),
		CFrame.new((branchStart + anchorPos) / 2, anchorPos) * CFrame.Angles(0, math.rad(90), 0),
		BARK,
		Enum.Material.Wood,
		tree
	)

	-- COPA de folhagem
	local canopy = base + Vector3.new(0, 27, 0)
	local leafBlobs = {
		{ Vector3.new(0, 0, 0), 18, LEAF_B },
		{ Vector3.new(7, -2, 4), 13, LEAF_A },
		{ Vector3.new(-7, -1, -3), 13, LEAF_C },
		{ Vector3.new(4, 3, -6), 12, LEAF_C },
		{ Vector3.new(-5, 2, 6), 12, LEAF_A },
		{ Vector3.new(0, 5, 0), 12, LEAF_B },
	}
	for _, lb in leafBlobs do
		local off, d, col = lb[1] :: Vector3, lb[2] :: number, lb[3] :: Color3
		blob(Vector3.new(d, d, d), canopy + off, col, tree)
	end
end

-- ======================================================================
--  CIPÓ 🌿 (função reaproveitável) — pêndulo com física de rótula
-- ======================================================================
local function makeVine(anchorPos: Vector3, length: number, index: number)
	-- ponto fixo no galho (âncora invisível)
	local pivot = make("Part", {
		Name = "VinePivot" .. index,
		Size = Vector3.new(1, 1, 1),
		Position = anchorPos,
		Anchored = true,
		Transparency = 1,
		CanCollide = false,
	}) :: BasePart
	local pivotAtt = make("Attachment", { Parent = pivot }) :: Attachment

	-- o cipó: cilindro comprido, SOLTO, preso ao pivô por uma rótula
	-- (BallSocket) — balança livre em qualquer direção como pêndulo real.
	local vine = make("Part", {
		Name = "Vine" .. index,
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(length, 0.7, 0.7),
		CFrame = CFrame.new(anchorPos - Vector3.new(0, length / 2, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Color = VINE_COLOR,
		Material = Enum.Material.Grass,
		Anchored = false,
		CanCollide = false,
	}) :: BasePart

	-- attachment no TOPO do cipó (o comprimento é o eixo X local)
	local vineTopAtt = make("Attachment", {
		Parent = vine,
		Position = Vector3.new(length / 2, 0, 0),
	}) :: Attachment

	-- rótula: liga o topo do cipó ao pivô fixo -> vira um pêndulo
	make("BallSocketConstraint", {
		Parent = vine,
		Attachment0 = pivotAtt,
		Attachment1 = vineTopAtt,
	})

	-- folhinhas decorativas ao longo do cipó
	for i = 1, 3 do
		blob(Vector3.new(2, 2, 2), anchorPos - Vector3.new(0, i * (length / 4), 0), LEAF_A)
	end

	-- marca com a tag pro script de agarrar reconhecer
	CollectionService:AddTag(vine, "Grabbable")
	return vine
end

-- ======================================================================
--  MONTAGEM FINAL
-- ======================================================================
-- pontos de onde os cipós penduram (sobre a água, alinhados no caminho)
local VINE_A_ANCHOR = Vector3.new(0, 25, 22)
local VINE_B_ANCHOR = Vector3.new(0, 25, -6)

makeTree(TREE_A_BASE, VINE_A_ANCHOR)
makeTree(TREE_B_BASE, VINE_B_ANCHOR)

makeVine(VINE_A_ANCHOR, 15, 1)
makeVine(VINE_B_ANCHOR, 15, 2)

-- ======================================================================
--  TROFÉU 🏆 na ilha de chegada
-- ======================================================================
local trophyBase = GOAL_ISLAND + Vector3.new(0, 2, 0)
make("Part", {
	Name = "TrophyStand",
	Size = Vector3.new(3, 2, 3),
	Position = trophyBase + Vector3.new(0, 1, 0),
	Color = Color3.fromRGB(120, 90, 55),
	Material = Enum.Material.WoodPlanks,
	Anchored = true,
})
local cup = make("Part", {
	Name = "Trophy",
	Shape = Enum.PartType.Ball,
	Size = Vector3.new(3, 4, 3),
	Position = trophyBase + Vector3.new(0, 4, 0),
	Color = Color3.fromRGB(255, 208, 60),
	Material = Enum.Material.Neon,
	Anchored = true,
	CanCollide = false,
}) :: BasePart
make("PointLight", { Parent = cup, Color = Color3.fromRGB(255, 220, 120), Brightness = 3, Range = 22 })

print("[Cena] 🐊🌿 Lago, jacarés, 2 árvores e 2 cipós prontos! Balance de cipó em cipó até o troféu.")
