rsc235_protocol = Proto("235",  "RSC235 Protocol")

-- Custom opcodes
VIRTUAL_OPCODE_CONNECT = 10000

-- Server opcodes
SERVER_OPCODE_CONNECTION_CLOSE_NOTIFY = 4
SERVER_OPCODE_PRIVACY_SETTINGS = 51
SERVER_OPCODE_CONNECTION_CLOSE = 165
SERVER_OPCODE_LOGOUT_DENY = 183
SERVER_OPCODE_BANK_CLOSE = 203
SERVER_OPCODE_SETTINGS = 240
SERVER_OPCODE_OPTIONS_OPEN = 245
SERVER_OPCODE_OPTIONS_CLOSE = 252

-- Client opcodes
CLIENT_OPCODE_CONNECT = 0
CLIENT_OPCODE_KEEPALIVE = 67
CLIENT_OPCODE_LOGOUT = 102
CLIENT_OPCODE_OPTIONS_SELECT = 116
CLIENT_OPCODE_BANK_CLOSE = 212

clientPacket = ProtoField.uint8("rsc235.clientPacket", "Type", base.HEX)
clientOpcode = ProtoField.uint32("rsc235.clientOpcode", "Opcode", base.DEC)
clientPacketLength = ProtoField.string("rsc235.clientPacketLength", "Packet Length")
clientOption = ProtoField.uint8("rsc235.clientOption", "Option", base.DEC)

serverLoginResponse = ProtoField.int8("rsc235.serverLoginResponse", "Login Response", base.DEC)
serverBlockChat = ProtoField.uint8("rsc235.serverBlockChat", "Block Chat Messages", base.HEX)
serverBlockPrivate = ProtoField.uint8("rsc235.serverBlockPrivate", "Block Private Messages", base.HEX)
serverBlockTrade = ProtoField.uint8("rsc235.serverBlockTrade", "Block Trade Requests", base.HEX)
serverBlockDuel = ProtoField.uint8("rsc235.serverBlockDuel", "Block Duel Requests", base.HEX)
serverCameraModeAuto = ProtoField.uint8("rsc235.serverCameraModeAuto", "Camera Angle Mode", base.HEX)
serverMouseButtonOne = ProtoField.uint8("rsc235.serverMouseButtonOne", "Mouse Buttons", base.HEX)
serverSoundDisabled = ProtoField.uint8("rsc235.serverSoundDisabled", "Sound Effects", base.HEX)

commonCount = ProtoField.uint32("rsc235.commonCount", "Count", base.DEC)
commonString = ProtoField.string("rsc235.serverOption", "String")

rsc235_protocol.fields = {
	clientPacket,
	clientOpcode,
	clientPacketLength,
	clientOption,
	
	serverLoginResponse,
	serverBlockChat,
	serverBlockPrivate,
	serverBlockTrade,
	serverBlockDuel,
	serverCameraModeAuto,
	serverMouseButtonOne,
	serverSoundDisabled,
	
	commonCount,
	commonString
}

function resolveOpcodeName(clientPacket, opcode)
	if (clientPacket == 1) then
		return "CLIENT_" .. resolveClientOpcodeName(opcode)
	else
		return "SERVER_" .. resolveServerOpcodeName(opcode)
	end
end

function resolveClientOpcodeName(opcode)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		return "VIRTUAL_OPCODE_CONNECT"
	elseif (opcode == CLIENT_OPCODE_CONNECT) then
		return "OPCODE_CONNECT"
	elseif (opcode == CLIENT_OPCODE_KEEPALIVE) then
		return "OPCODE_KEEPALIVE";
	elseif (opcode == CLIENT_OPCODE_LOGOUT) then
		return "OPCODE_LOGOUT";
	elseif (opcode == CLIENT_OPCODE_OPTIONS_SELECT) then
		return "OPCODE_OPTIONS_SELECT";
	elseif (opcode == CLIENT_OPCODE_BANK_CLOSE) then
		return "OPCODE_BANK_CLOSE";
	else
		return "OPCODE_UNKNOWN"
	end
end

function resolveServerOpcodeName(opcode)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		return "VIRTUAL_OPCODE_CONNECT"
	elseif (opcode == SERVER_OPCODE_CONNECTION_CLOSE_NOTIFY) then
		return "OPCODE_CONNECTION_CLOSE_NOTIFY"
	elseif (opcode == SERVER_OPCODE_PRIVACY_SETTINGS) then
		return "OPCODE_PRIVACY_SETTINGS"
	elseif (opcode == SERVER_OPCODE_CONNECTION_CLOSE) then
		return "OPCODE_CONNECTION_CLOSE"
	elseif (opcode == SERVER_OPCODE_LOGOUT_DENY) then
		return "OPCODE_LOGOUT_DENY"
	elseif (opcode == SERVER_OPCODE_BANK_CLOSE) then
		return "OPCODE_BANK_CLOSE"
	elseif (opcode == SERVER_OPCODE_SETTINGS) then
		return "OPCODE_SETTINGS"
	elseif (opcode == SERVER_OPCODE_OPTIONS_OPEN) then
		return "OPCODE_OPTIONS_OPEN"
	elseif (opcode == SERVER_OPCODE_OPTIONS_CLOSE) then
		return "OPCODE_OPTIONS_CLOSE"
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
	local packetLengthBuffer = rsc_getPacketLengthBuffer(buffer, 0)
	local packetLength = rsc_readPacketLength(packetLengthBuffer)
	
	-- Offset buffer by length size
	buffer = buffer(packetLengthBuffer:len())

	local clientOpcodeData = buffer(0,1)
	local opcodeName = resolveClientOpcodeName(clientOpcodeData:uint())
	tree:add(clientPacketLength, packetLengthBuffer, packetLength)
	local opcodeField = tree:add(clientOpcode, clientOpcodeData)
	opcodeField:append_text(" (" .. opcodeName .. ")");
	
	local clientOpcodeValue = clientOpcodeData:uint()
	if (clientOpcodeValue == CLIENT_OPCODE_CONNECT) then
		-- TODO: Show outgoing connect packet
	elseif (clientOpcodeValue == CLIENT_OPCODE_OPTIONS_SELECT) then
		local option = buffer(1,1)
		tree:add(clientOption, option)
	end
end

function addOpcodeDataServer(opcode, tree, buffer)
	if (opcode == VIRTUAL_OPCODE_CONNECT) then
		local loginResponse = buffer(0,1)
		tree:add(serverLoginResponse, loginResponse)
	else
		local packetLengthBuffer = rsc_getPacketLengthBuffer(buffer, 0)
		local packetLength = rsc_readPacketLength(packetLengthBuffer)
		
		-- Offset buffer by length size
		buffer = buffer(packetLengthBuffer:len())
		
		local serverOpcode = buffer(packetOffset,1)
		local opcodeName = resolveServerOpcodeName(serverOpcode:uint())
		tree:add(clientPacketLength, packetLengthBuffer, packetLength)
		local opcodeField = tree:add(clientOpcode, serverOpcode)
		opcodeField:append_text(" (" .. opcodeName .. ")");
		
		local serverOpcodeValue = serverOpcode:uint()
		if (serverOpcodeValue == SERVER_OPCODE_PRIVACY_SETTINGS) then
			local blockChat = buffer(1,1)
			local blockPrivate = buffer(2,1)
			local blockTrade = buffer(3,1)
			local blockDuel = buffer(4,1)
			local field = tree:add(serverBlockChat, blockChat)
			if (blockChat:uint() == 0) then
				field:append_text(" (Off)")
			else
				field:append_text(" (On)")
			end
			field = tree:add(serverBlockPrivate, blockPrivate)
			if (blockPrivate:uint() == 0) then
				field:append_text(" (Off)")
			else
				field:append_text(" (On)")
			end
			field = tree:add(serverBlockTrade, blockTrade)
			if (blockTrade:uint() == 0) then
				field:append_text(" (Off)")
			else
				field:append_text(" (On)")
			end
			field = tree:add(serverBlockDuel, blockDuel)
			if (blockDuel:uint() == 0) then
				field:append_text(" (Off)")
			else
				field:append_text(" (On)")
			end
		elseif (serverOpcodeValue == SERVER_OPCODE_SETTINGS) then
			local cameraModeAuto = buffer(1,1)
			local mouseModeOne = buffer(2,1)
			local disableSound = buffer(3,1)
			local field = tree:add(serverCameraModeAuto, cameraModeAuto)
			if (cameraModeAuto:uint() == 1) then
				field:append_text(" (Auto)")
			else
				field:append_text(" (Manual)")
			end
			field = tree:add(serverMouseButtonOne, mouseModeOne)
			if (mouseModeOne:uint() == 1) then
				field:append_text(" (One)")
			else
				field:append_text(" (Two)")
			end
			field = tree:add(serverSoundDisabled, disableSound)
			if (disableSound:uint() == 1) then
				field:append_text(" (Disabled)")
			else
				field:append_text(" (Enabled)")
			end
		elseif (serverOpcodeValue == SERVER_OPCODE_OPTIONS_OPEN) then
			local optionCount = buffer(1,1)
			tree:add(commonCount, optionCount)
			
			local offset = 2;
			for i = 0, optionCount:uint() - 1, 1 do
				local stringBuffer = rsc_getRSCStringBuffer(buffer, offset)
				local stringValue = rsc_readRSCString(stringBuffer)
				local stringTree = tree:add(commonString, stringBuffer, "")
				stringTree:set_text("Option (" .. i .. "): " .. stringValue)
				offset = offset + stringBuffer:len()
			end
		end
	end
end

function rsc_getPacketLengthBuffer(buffer, offset)
	local length = buffer(offset,1)
	if (length:uint() >= 160) then
		length = buffer(offset,2)
	end
	return length
end

function rsc_getRSCStringBuffer(buffer, offset)
	local length = 1;
	local value = buffer(offset + 1,1)
	while (value:uint() ~= 0) do
		length = length + 1
		value = buffer(offset + length,1)
	end
	length = length + 1
	return buffer(offset, length)
end

function rsc_readPacketLength(buffer)
	local length = buffer(0,1):uint()
	if (buffer:len() > 1) then
		length = 256 * length - (40960 - buffer(1,1):uint())
	end
	return length
end

function rsc_readRSCString(buffer)
	return buffer(1,buffer:len() - 2):string()
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
	local clientPacketField = header_subtree:add(clientPacket, clientPacketValue)
	if (clientPacketValue:uint() == 1) then
		clientPacketField:append_text(" (Client)");
	else
		clientPacketField:append_text(" (Server)");
	end
	
	local clientOpcodeField = header_subtree:add(clientOpcode, clientOpcodeValue)
	clientOpcodeField:append_text(" (" .. opcodeName .. ")");
	
	local data_buffer = buffer(5)
	local data_subtree = tree:add(rsc235_protocol, data_buffer, "RSC235 Data")
	addOpcodeData(clientPacketValue:uint(), clientOpcodeValue:uint(), data_subtree, data_buffer)
end

local ethertype_table = DissectorTable.get("ethertype")
ethertype_table:add(0x0, rsc235_protocol)
