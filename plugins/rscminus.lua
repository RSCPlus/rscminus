rsc235_protocol = Proto("235",  "RSC235 Protocol")

-- Custom opcodes
VIRTUAL_OPCODE_CONNECT = 10000

-- Server opcodes
SERVER_OPCODE_PRIVACY_SETTINGS = 51

-- Client opcodes
CLIENT_OPCODE_CONNECT = 0

clientPacket = ProtoField.bool("rsc235.clientPacket", "Client Packet")
clientOpcode = ProtoField.uint32("rsc235.clientOpcode", "Opcode", base.DEC)

serverLoginResponse = ProtoField.int8("rsc235.serverLoginResponse", "Login Response", base.DEC)
serverBlockChat = ProtoField.bool("rsc235.blockChat", "Block Chat Messages")
serverBlockPrivate = ProtoField.bool("rsc235.blockPrivate", "Block Private Messages")
serverBlockTrade = ProtoField.bool("rsc235.blockTrade", "Block Trade Requests")
serverBlockDuel = ProtoField.bool("rsc235.blockDuel", "Block Duel Requests")

rsc235_protocol.fields = {
	clientPacket,
	clientOpcode,
	serverLoginResponse,
	serverBlockChat,
	serverBlockPrivate,
	serverBlockTrade,
	serverBlockDuel
}

function resolveOpcodeName(clientPacket, opcode)
	if (clientPacket == 1) then
		return resolveClientOpcodeName(opcode)
	else
		return resolveServerOpcodeName(opcode)
	end
end

function resolveClientOpcodeName(opcode)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		return "VIRTUAL_OPCODE_CONNECT"
	elseif (opcode == CLIENT_OPCODE_CONNECT) then
		return "OPCODE_CONNECT"
	else
		return "OPCODE_UNKNOWN"
	end
end

function resolveServerOpcodeName(opcode)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		return "VIRTUAL_OPCODE_CONNECT"
	elseif (opcode == SERVER_OPCODE_PRIVACY_SETTINGS) then
		return "OPCODE_PRIVACY_SETTINGS"
	else
		return "OPCODE_UNKNOWN"
	end
end

function addOpcodeData(clientPacket, opcode, tree, buffer)
	if (clientPacket == 1) then
		addOpcodeDataClient(opcode, tree, buffer)
	else
		addOpcodeDataServer(opcode, tree, buffer)
	end
end

function addOpcodeDataClient(opcode, tree, buffer)
	local clientOpcodeData = buffer(0,1)
	local opcodeName = resolveClientOpcodeName(clientOpcodeData:uint())
	local opcodeField = tree:add(clientOpcode, clientOpcodeData)
	opcodeField:append_text(" (" .. opcodeName .. ")");
	
	local clientOpcodeValue = clientOpcodeData:uint()
	if (clientOpcodeValue == CLIENT_OPCODE_CONNECT) then
		-- TODO: Show outgoing connect packet
	end
end

function addOpcodeDataServer(opcode, tree, buffer)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		local loginResponse = buffer(0,1)
		tree:add(serverLoginResponse, loginResponse)
	else
		local serverOpcode = buffer(0,1)
		local opcodeName = resolveServerOpcodeName(serverOpcode:uint())
		local opcodeField = tree:add(clientOpcode, serverOpcode)
		opcodeField:append_text(" (" .. opcodeName .. ")");
		
		local serverOpcodeValue = serverOpcode:uint()
		if (serverOpcodeValue == SERVER_OPCODE_PRIVACY_SETTINGS) then
			local blockChat = buffer(1,1)
			local blockPrivate = buffer(2,1)
			local blockTrade = buffer(3,1)
			local blockDuel = buffer(4,1)
			tree:add(serverBlockChat, blockChat)
			tree:add(serverBlockPrivate, blockPrivate)
			tree:add(serverBlockTrade, blockTrade)
			tree:add(serverBlockDuel, blockDuel)
		end
	end
end

function rsc235_protocol.dissector(buffer, pinfo, tree)
	length = buffer:len()
	if length == 0 then return end
	
	pinfo.cols.protocol = rsc235_protocol.name
	  
	local clientPacketValue = buffer(0,1)
	local clientOpcodeValue = buffer(1,4)
	local opcodeName = resolveOpcodeName(clientPacketValue:uint(), clientOpcodeValue:uint())
	
	pinfo.cols['info'] = opcodeName
	
	local header_subtree = tree:add(rsc235_protocol, buffer(0,5), "rscminus Header")
	header_subtree:add(clientPacket, clientPacketValue)
	local clientOpcodeField = header_subtree:add(clientOpcode, clientOpcodeValue)
	clientOpcodeField:append_text(" (" .. opcodeName .. ")");
	
	local data_buffer = buffer(5)
	local data_subtree = tree:add(rsc235_protocol, data_buffer, "RSC235 Data")
	addOpcodeData(clientPacketValue:uint(), clientOpcodeValue:uint(), data_subtree, data_buffer)
end

local ethertype_table = DissectorTable.get("ethertype")
ethertype_table:add(0x0, rsc235_protocol)
