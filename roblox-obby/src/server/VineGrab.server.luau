--!strict
-- VineGrab.server.luau  ->  vai em ServerScriptService
-- =========================================================================
-- Faz o cipó (parte marcada com "Grabbable") ser AGARRÁVEL:
--   * Encoste no cipó  -> seu personagem se pendura e começa a balançar
--   * Aperte ESPAÇO    -> solta e é lançado com o embalo (dá pra "voar")
--
-- Funciona com qualquer parte marcada "Grabbable", então dá pra criar vários
-- cipós depois. 🌿
-- =========================================================================

local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

-- RemoteEvent que o cliente usa pra avisar "quero soltar" (ao apertar espaço)
local releaseEvent = ReplicatedStorage:FindFirstChild("VineRelease") :: RemoteEvent?
if not releaseEvent then
	releaseEvent = Instance.new("RemoteEvent")
	releaseEvent.Name = "VineRelease"
	releaseEvent.Parent = ReplicatedStorage
end

type Hold = {
	vine: BasePart,
	weld: Weld,
	humanoid: Humanoid,
	hrp: BasePart,
}

local holding: { [Player]: Hold } = {}
local cooldownUntil: { [Player]: number } = {}

-- solta o cipó, devolvendo o controle e mantendo o embalo (impulso)
local function release(player: Player)
	local hold = holding[player]
	if not hold then
		return
	end
	holding[player] = nil

	local hrp = hold.hrp
	local momentum = hrp.AssemblyLinearVelocity

	hold.weld:Destroy()
	hold.humanoid.PlatformStand = false

	-- avisa o cliente pra TIRAR a pose de braço erguido
	local character = hold.humanoid.Parent
	if character then
		character:SetAttribute("OnVine", false)
	end

	-- devolve a "posse" física do cipó pro servidor
	pcall(function()
		hold.vine:SetNetworkOwner(nil)
	end)

	-- aplica o embalo do balanço + um empurrãozinho pra cima = pulo estiloso.
	-- multiplica o embalo horizontal um pouco pra alcançar o próximo cipó 🌿→🌿
	local flat = Vector3.new(momentum.X, 0, momentum.Z) * 1.15
	hrp.AssemblyLinearVelocity = flat + Vector3.new(0, math.max(momentum.Y, 0) + 26, 0)

	-- cooldown curto: impede re-grudar no MESMO cipó na hora, mas curto o
	-- bastante pra você agarrar o PRÓXIMO cipó no meio do voo.
	cooldownUntil[player] = os.clock() + 0.35
end

-- agarra o cipó
local function grab(player: Player, vine: BasePart, character: Model)
	if holding[player] then
		return
	end
	if os.clock() < (cooldownUntil[player] or 0) then
		return
	end
	local humanoid = character:FindFirstChildOfClass("Humanoid")
	local hrp = character:FindFirstChild("HumanoidRootPart") :: BasePart?
	if not humanoid or not hrp or humanoid.Health <= 0 then
		return
	end

	-- posiciona o personagem pendurado logo abaixo da ponta de baixo do cipó
	local bottom = vine.CFrame * CFrame.new(-(vine.Size.X / 2), 0, 0) -- ponta inferior
	local hangCFrame = CFrame.new(bottom.Position - Vector3.new(0, 2.5, 0))
	hrp.CFrame = hangCFrame

	-- entrega o controle físico do cipó pro cliente do jogador (balanço fluido)
	pcall(function()
		vine:SetNetworkOwner(player)
	end)

	-- prende o personagem no cipó
	local weld = Instance.new("Weld")
	weld.Part0 = vine
	weld.Part1 = hrp
	weld.C0 = vine.CFrame:ToObjectSpace(hrp.CFrame)
	weld.Parent = vine

	humanoid.PlatformStand = true -- desliga o andar; a física do cipó comanda

	-- liga a POSE de braço erguido no cliente (ArmPose.client.luau escuta isso)
	character:SetAttribute("OnVine", true)

	-- empurrãozinho inicial na direção pra onde o jogador olha, pra já balançar
	local look = hrp.CFrame.LookVector
	vine.AssemblyLinearVelocity = Vector3.new(look.X, 0, look.Z).Unit * 14

	holding[player] = {
		vine = vine,
		weld = weld,
		humanoid = humanoid,
		hrp = hrp,
	}
end

-- liga o "detector de toque" num cipó
local function connectVine(vine: BasePart)
	vine.Touched:Connect(function(hit)
		local character = hit.Parent :: Model?
		if not character then
			return
		end
		local player = Players:GetPlayerFromCharacter(character)
		if player then
			grab(player, vine, character)
		end
	end)
end

for _, inst in CollectionService:GetTagged("Grabbable") do
	if inst:IsA("BasePart") then
		connectVine(inst)
	end
end
CollectionService:GetInstanceAddedSignal("Grabbable"):Connect(function(inst)
	if inst:IsA("BasePart") then
		connectVine(inst)
	end
end)

-- pedido de soltar (veio do cliente ao apertar espaço)
releaseEvent.OnServerEvent:Connect(function(player)
	release(player)
end)

-- limpeza: se morrer ou sair enquanto pendurado, solta e limpa
Players.PlayerRemoving:Connect(function(player)
	holding[player] = nil
	cooldownUntil[player] = nil
end)
Players.PlayerAdded:Connect(function(player)
	player.CharacterRemoving:Connect(function()
		holding[player] = nil
	end)
end)
