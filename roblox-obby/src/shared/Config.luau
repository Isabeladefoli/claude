--!strict
-- Config.luau
-- =========================================================================
-- Aqui ficam TODOS os "botões de ajuste" do jogo.
-- Quer deixar a lava mais mortal? A plataforma mais rápida? É tudo aqui.
-- Mexa nos números, dê play, e veja o resultado. Isso é vibecoding. 🎮
-- =========================================================================

local Config = {}

-- Nomes das tags que você vai colocar nas partes dentro do Roblox Studio.
-- (Use a aba "Tags" no Studio, ou o plugin "Tag Editor").
Config.Tags = {
	Checkpoint = "Checkpoint", -- parte que salva o progresso do jogador
	Killer = "Killer", -- lava / espinhos: mata quem encostar
	MovingPlatform = "MovingPlatform", -- plataforma que vai e volta
	Disappearing = "Disappearing", -- plataforma que some quando você pisa
	Finish = "Finish", -- linha de chegada
	Coin = "Coin", -- moeda que o jogador coleta
	JumpPad = "JumpPad", -- trampolim: lança o jogador pro alto
	SpeedBoost = "SpeedBoost", -- deixa o jogador mais rápido por um tempo
	Spinner = "Spinner", -- obstáculo que gira sem parar
}

-- Plataforma que se move (vai e volta)
Config.MovingPlatform = {
	distance = 16, -- quantos studs ela anda
	time = 2, -- segundos pra ir de um lado ao outro
	axis = Vector3.new(1, 0, 0), -- direção do movimento (X aqui). Use (0,0,1) pra Z, etc.
}

-- Plataforma que some quando você pisa
Config.Disappearing = {
	delayBeforeVanish = 0.4, -- segundos depois de pisar até sumir
	respawnTime = 2, -- segundos até voltar
	warningTransparency = 0.5, -- fica "meio transparente" avisando antes de sumir
}

-- Moedas
Config.Coin = {
	value = 1, -- quantas moedas cada uma vale
	spinSpeed = 90, -- graus por segundo girando
	respawnTime = 8, -- segundos até a moeda voltar depois de coletada
}

-- Trampolim
Config.JumpPad = {
	power = 120, -- força do pulo (quanto maior, mais alto)
}

-- Faixa de velocidade
Config.SpeedBoost = {
	walkSpeed = 48, -- velocidade turbinada (a normal é 16)
	duration = 3, -- segundos que o efeito dura
}

-- Obstáculo giratório
Config.Spinner = {
	speed = 120, -- graus por segundo
	axis = Vector3.new(0, 1, 0), -- eixo de rotação (Y = gira em pé como um cata-vento deitado)
}

-- Efeitos de morte / respawn
Config.Respawn = {
	killMessageEnabled = true,
}

return Config
