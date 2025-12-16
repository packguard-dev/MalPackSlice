package Type.Source

import Type.{CodeType, CallType, RegexType, TypeDefinition}

object SourceGroups {

  /**
   * NHÓM 1: CÁC HÀM XỬ LÝ FILE (FILE OPERATIONS)
   */
  private val FILE_OPERATIONS: Seq[TypeDefinition] = Seq(
    RegexType("readFile"),       
    RegexType("readFileSync"),
    RegexType("read"),           
    RegexType("readSync"),
    RegexType("readv"),
    RegexType("readvSync"),
    RegexType("createReadStream"),
    RegexType("open"),
    RegexType("openSync"),
    RegexType("openAsBlob"),
    RegexType("opendir"),
    RegexType("opendirSync"),
    RegexType("readdir"),
    RegexType("readdirSync"),
    RegexType("readlink"),
    RegexType("readlinkSync"),
    RegexType("realpath"),
    RegexType("realpathSync"),
    
    RegexType("Dir"),
    RegexType("Dirent"),
    RegexType("ReadStream"),
    RegexType("FileReadStream"),

    RegexType("access"),
    RegexType("accessSync"),
    RegexType("exists"),
    RegexType("existsSync"),
    RegexType("stat"),
    RegexType("statSync"),
    RegexType("fstat"),
    RegexType("fstatSync"),
    RegexType("lstat"),
    RegexType("lstatSync"),
    RegexType("statfs"),
    RegexType("statfsSync"),
    RegexType("Stats"),
    RegexType("_toUnixTimestamp")
  )

  /**
   * NHÓM 2: THU THẬP THÔNG TIN (INFORMATION GATHERING)
   */
  private val INFORMATION_GATHERING: Seq[TypeDefinition] = Seq(
    // ----- OS Module (Methods) -----
    RegexType("userInfo"),       // os.userInfo
    RegexType("networkInterfaces"),
    RegexType("cpus"),
    RegexType("homedir"),
    RegexType("platform"),
    RegexType("hostname"),
    RegexType("arch"),
    RegexType("release"),
    RegexType("type"),
    RegexType("version"),        // os.version, process.version
    RegexType("machine"),
    RegexType("tmpdir"),
    RegexType("totalmem"),
    RegexType("freemem"),
    RegexType("loadavg"),
    RegexType("uptime"),         // os.uptime, process.uptime
    RegexType("endianness"),
    RegexType("getPriority"),
    RegexType("availableParallelism"),

    // ----- Process Module (Methods) -----
    RegexType("cwd"),            // process.cwd()
    RegexType("getuid"),
    RegexType("geteuid"),
    RegexType("getgid"),
    RegexType("getegid"),
    RegexType("getgroups"),
    RegexType("cpuUsage"),
    RegexType("memoryUsage"),
    RegexType("resourceUsage"),
    RegexType("constrainedMemory"),
    RegexType("hrtime"),
    RegexType("openStdin"),
    RegexType("getActiveResourcesInfo"),
    RegexType("_getActiveRequests"),
    RegexType("_getActiveHandles"),

    // ----- Process Properties (Variables/Fields) -----
    CodeType("process.env"),
    CodeType("process.argv"),
    CodeType("process.version"),
    CodeType("process.pid"),
    CodeType("process.platform"),
    CodeType("process.arch"),

    // ----- DNS Module (Methods) -----
    RegexType("lookup"),
    RegexType("lookupService"),
    RegexType("resolve"),
    RegexType("resolve4"),
    RegexType("resolve6"),
    RegexType("resolveAny"),
    RegexType("resolveCaa"),
    RegexType("resolveCname"),
    RegexType("resolveMx"),
    RegexType("resolveNaptr"),
    RegexType("resolveNs"),
    RegexType("resolvePtr"),
    RegexType("resolveSoa"),
    RegexType("resolveSrv"),
    RegexType("resolveTxt"),
    RegexType("reverse"),
    RegexType("getServers"),
    RegexType("Resolver")
  )

  /**
   * NHÓM 3: GIAO TIẾP MẠNG (NETWORK COMMUNICATION)
   */
  private val NETWORK_COMMUNICATION: Seq[TypeDefinition] = Seq(
    // HTTP/HTTPS/Net Methods
    RegexType("createServer"),         // http.createServer, net.createServer...
    RegexType("createSecureServer"),   // http2...
    RegexType("createConnection"),     // net.createConnection
    RegexType("connect"),              // net.connect, http2.connect
    RegexType("createSecurePair"),
    RegexType("createSocket"),         // dgram.createSocket
    
    // Client Requests
    RegexType("get"),                  // http.get, https.get
    RegexType("request"),              // http.request
    RegexType("fetch"),                // global.fetch

    // Classes / Constructors
    RegexType("Server"),
    RegexType("IncomingMessage"),
    RegexType("ServerResponse"),
    RegexType("Socket"),
    RegexType("Stream"),
    RegexType("TLSSocket"),
    RegexType("WebSocket"),
    
    // Internal / Specific
    RegexType("_connectionListener"),
    RegexType("_createServerHandle"),
    RegexType("_setSimultaneousAccepts"),
    RegexType("send"),                
    RegexType("Http2ServerRequest"),
    RegexType("Http2ServerResponse")
  )

  def getAllSources: Set[TypeDefinition] = {
    (FILE_OPERATIONS ++
      INFORMATION_GATHERING ++
      NETWORK_COMMUNICATION).toSet
  }
}