--!strict
-- World.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Monta a cena: chão de grama, uma ÁRVORE caprichada e um CIPÓ pendurado
-- num galho, pronto pra você se segurar e balançar.
--
-- A lógica de "agarrar / soltar" o cipó fica no VineGrab.server.luau.
-- =========================================================================

local CollectionService = game:GetService("CollectionService")

-- cores naturais
local BARK = Color3.fromRGB(92, 62, 41)
local BARK_DARK = Color3.fromRGB(72, 48, 32)
local LEAF_A = Color3.fromRGB(60, 110, 45)
local LEAF_B = Color3.fromRGB(84, 142, 58)
local LEAF_C = Color3.fromRGB(104, 168, 74)
local VINE_COLOR = Color3.fromRGB(74, 120, 52)

local world = Instance.new("Folder")
world.Name = "World"
world.Parent = workspace

-- helper de bloco genérico
local function make(class: string, props: { [string]: any }): Instance
	local inst = Instance.new(class)
	for k, v in props do
		(inst :: any)[k] = v
	end
	inst.Parent = props.Parent or world
	return inst
end

local function cylinder(size: Vector3, cframe: CFrame, color: Color3, material: Enum.Material): BasePart
	return make("Part", {
		Shape = Enum.PartType.Cylinder,
		Size = size,
		CFrame = cframe,
		Color = color,
		Material = material,
		Anchored = true,
		TopSurface = Enum.SurfaceType.Smooth,
		BottomSurface = Enum.SurfaceType.Smooth,
	}) :: BasePart
end

local function blob(size: Vector3, pos: Vector3, color: Color3): BasePart
	return make("Part", {
		Shape = Enum.PartType.Ball,
		Size = size,
		Position = pos,
		Color = color,
		Material = Enum.Material.Grass,
		Anchored = true,
		CanCollide = false,
	}) :: BasePart
end

-- ======================================================================
--  CHÃO
-- ======================================================================
make("Part", {
	Name = "Ground",
	Size = Vector3.new(200, 4, 200),
	Position = Vector3.new(0, -2, 0),
	Color = Color3.fromRGB(86, 138, 62),
	Material = Enum.Material.Grass,
	Anchored = true,
})

-- ponto de nascimento
make("SpawnLocation", {
	Size = Vector3.new(8, 1, 8),
	Position = Vector3.new(0, 0.5, 18),
	Color = Color3.fromRGB(110, 165, 85),
	Material = Enum.Material.Grass,
	Anchored = true,
})

-- ======================================================================
--  A ÁRVORE 🌳
-- ======================================================================
local treeBase = Vector3.new(0, 0, 0)

-- tronco: cilindros empilhados que vão afinando (dá a impressão de tronco real)
local trunkSections = {
	{ y = 3, r = 4.0 },
	{ y = 9, r = 3.4 },
	{ y = 15, r = 2.8 },
	{ y = 20, r = 2.2 },
}
for _, s in trunkSections do
	-- cilindro em pé = girar 90° no eixo Z (o comprimento do cilindro é o eixo X)
	local height = 7
	cylinder(
		Vector3.new(height, s.r * 2, s.r * 2),
		CFrame.new(treeBase + Vector3.new(0, s.y, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		s.y < 12 and BARK or BARK_DARK,
		Enum.Material.Wood
	)
end

-- raízes na base (pequenos cilindros inclinados saindo do tronco)
for i = 0, 5 do
	local ang = math.rad(i * 60)
	local dir = Vector3.new(math.cos(ang), 0, math.sin(ang))
	local rootPos = treeBase + dir * 4 + Vector3.new(0, 0.6, 0)
	cylinder(
		Vector3.new(5, 1.6, 1.6),
		CFrame.new(rootPos, rootPos + dir + Vector3.new(0, -0.4, 0)) * CFrame.Angles(0, math.rad(90), 0),
		BARK_DARK,
		Enum.Material.Wood
	)
end

-- galhos principais saindo do alto do tronco
local branches = {
	{ dir = Vector3.new(1, 0.5, 0.2), len = 12 },
	{ dir = Vector3.new(-0.8, 0.6, -0.3), len = 11 },
	{ dir = Vector3.new(0.1, 0.7, 1), len = 10 },
	{ dir = Vector3.new(-0.2, 0.6, -1), len = 9 },
}
for _, b in branches do
	local dir = b.dir.Unit
	local start = treeBase + Vector3.new(0, 20, 0)
	local finish = start + dir * b.len
	local mid = (start + finish) / 2
	cylinder(
		Vector3.new(b.len, 1.8, 1.8),
		CFrame.new(mid, finish) * CFrame.Angles(0, math.rad(90), 0),
		BARK,
		Enum.Material.Wood
	)
end

-- galho FORTE e mais horizontal, de onde o cipó vai pendurar
local vineBranchStart = treeBase + Vector3.new(0, 22, 0)
local vineBranchEnd = vineBranchStart + Vector3.new(9, 1.5, 0)
cylinder(
	Vector3.new(11, 2.2, 2.2),
	CFrame.new((vineBranchStart + vineBranchEnd) / 2, vineBranchEnd) * CFrame.Angles(0, math.rad(90), 0),
	BARK,
	Enum.Material.Wood
)

-- COPA: várias bolotas de folha em tons diferentes (parece folhagem de verdade)
local canopyCenter = treeBase + Vector3.new(0, 26, 0)
local leafBlobs = {
	{ Vector3.new(0, 0, 0), 18, LEAF_B },
	{ Vector3.new(7, -2, 4), 13, LEAF_A },
	{ Vector3.new(-7, -1, -3), 13, LEAF_C },
	{ Vector3.new(4, 3, -6), 12, LEAF_C },
	{ Vector3.new(-5, 2, 6), 12, LEAF_A },
	{ Vector3.new(0, 5, 0), 12, LEAF_B },
	{ Vector3.new(9, 1, -2), 10, LEAF_B },
}
for _, lb in leafBlobs do
	local off, d, col = lb[1] :: Vector3, lb[2] :: number, lb[3] :: Color3
	blob(Vector3.new(d, d, d), canopyCenter + off, col)
end

-- ======================================================================
--  O CIPÓ 🌿  (pendura no galho e balança como um pêndulo)
-- ======================================================================
-- ponto fixo no galho (âncora do cipó)
local vineTopPos = Vector3.new(7, 21, 0)
local pivot = make("Part", {
	Name = "VinePivot",
	Size = Vector3.new(1, 1, 1),
	Position = vineTopPos,
	Anchored = true,
	Transparency = 1,
	CanCollide = false,
}) :: BasePart
local pivotAtt = make("Attachment", { Parent = pivot }) :: Attachment

-- o cipó em si: um cilindro comprido, SOLTO, preso ao pivô por uma junta de
-- "rótula" (BallSocket), que deixa ele balançar livre em qualquer direção.
local vineLength = 13
local vine = make("Part", {
	Name = "Vine",
	Shape = Enum.PartType.Cylinder,
	Size = Vector3.new(vineLength, 0.6, 0.6),
	-- posiciona o cipó pendurado pra baixo a partir do pivô
	CFrame = CFrame.new(vineTopPos - Vector3.new(0, vineLength / 2, 0)) * CFrame.Angles(0, 0, math.rad(90)),
	Color = VINE_COLOR,
	Material = Enum.Material.Grass,
	Anchored = false,
	CanCollide = false,
}) :: BasePart

-- attachment no TOPO do cipó (lembre: o comprimento é o eixo X local)
local vineTopAtt = make("Attachment", {
	Parent = vine,
	Position = Vector3.new(vineLength / 2, 0, 0),
}) :: Attachment

-- junta de rótula: liga o topo do cipó ao pivô fixo -> vira um pêndulo
make("BallSocketConstraint", {
	Parent = vine,
	Attachment0 = pivotAtt,
	Attachment1 = vineTopAtt,
})

-- marca o cipó com uma tag pro script de agarrar reconhecer
CollectionService:AddTag(vine, "Grabbable")

-- folhinhas decorativas no cipó
for i = 1, 3 do
	blob(Vector3.new(2, 2, 2), vineTopPos - Vector3.new(0, i * 3.5, 0), LEAF_A)
end

print("[Cena] 🌳 Árvore e cipó prontos! Encoste no cipó pra se pendurar e pule pra soltar.")
