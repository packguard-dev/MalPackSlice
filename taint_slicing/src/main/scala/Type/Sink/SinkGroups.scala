package Type.Sink

import Type.{CallType, RegexType, TypeDefinition}

object SinkGroups {

  /**
   * NHÓM 1: GHI FILE & THAO TÁC FILE (FILE MODIFICATION)
   */
  private val FILE_OPERATIONS: Seq[TypeDefinition] = Seq(
    // Write Operations
    RegexType("writeFile"),      // fs.writeFile, fs.promises.writeFile
    RegexType("writeFileSync"),
    RegexType("appendFile"),     // fs.appendFile
    RegexType("appendFileSync"),
    RegexType("createWriteStream"),
    RegexType("write"),          // fs.write, process.stdout.write
    RegexType("writeSync"),
    RegexType("writev"),
    RegexType("writevSync"),
    RegexType("save"),           // mongoose.save (thường là ghi DB nhưng cũng tính là IO sink)

    // Copy/Move Operations
    RegexType("copyFile"),
    RegexType("copyFileSync"),
    RegexType("cp"),
    RegexType("cpSync"),
    RegexType("rename"),
    RegexType("renameSync"),

    // Permission/Attribute Changes
    RegexType("chmod"),
    RegexType("chmodSync"),
    RegexType("chown"),
    RegexType("chownSync"),
    RegexType("lchown"),
    RegexType("lchownSync"),
    RegexType("fchmod"),
    RegexType("fchmodSync"),
    RegexType("fchown"),
    RegexType("fchownSync"),
    RegexType("utimes"),
    RegexType("utimesSync"),
    RegexType("futimes"),
    RegexType("futimesSync"),
    RegexType("lutimes"),
    RegexType("lutimesSync"),

    // Directory Operations
    RegexType("mkdir"),
    RegexType("mkdirSync"),
    RegexType("mkdtemp"),
    RegexType("mkdtempSync"),

    // Link Operations
    RegexType("symlink"),
    RegexType("symlinkSync"),
    RegexType("link"),
    RegexType("linkSync"),

    // Sync/Close
    RegexType("fdatasync"),
    RegexType("fdatasyncSync"),
    RegexType("fsync"),
    RegexType("fsyncSync"),
    RegexType("close"),
    RegexType("closeSync"),

    // Constructors
    RegexType("WriteStream"),
    RegexType("FileWriteStream")
  )

  /**
   * NHÓM 2: THỰC THI LỆNH HỆ THỐNG (RCE / COMMAND INJECTION)
   */
  private val SYSTEM_COMMAND_EXECUTION: Seq[TypeDefinition] = Seq(
    // child_process methods
    RegexType("exec"),           // child_process.exec, shelljs.exec
    RegexType("execSync"),
    RegexType("spawn"),          // child_process.spawn
    RegexType("spawnSync"),
    RegexType("fork"),           // child_process.fork
    RegexType("execFile"),
    RegexType("execFileSync"),
    RegexType("_forkChild"),
    RegexType("ChildProcess"),

    // Process methods
    RegexType("dlopen"),
    RegexType("binding"),
    RegexType("_linkedBinding"),

    // Dangerous JS Eval & Script Execution
    RegexType("eval"),           // eval(...)
    RegexType("Function"),       // new Function(...)
    RegexType("setTimeout"),     // setTimeout("code") - though less common in Node
    RegexType("setInterval"),
    
    // PowerShell / Shell Specific (ít gặp trong Node thuần nhưng có thể qua thư viện)
    RegexType("Invoke-Expression"),
    RegexType("Start-Process"),
    RegexType("Invoke-Command"),
    RegexType("ShellExecute"),
    RegexType("run")             // Common naming for task runners
  )

  /**
   * NHÓM 3: GIAO TIẾP MẠNG & CƠ SỞ DỮ LIỆU (EXFILTRATION / SQL INJECTION)
   */
  private val NETWORK_COMMUNICATION: Seq[TypeDefinition] = Seq(
    // HTTP Clients (Leak data via URL or Body)
    RegexType("request"),        // http.request, https.request
    RegexType("get"),            // http.get, axios.get (Cẩn thận False Positive với Map.get)
    RegexType("post"),           // axios.post, request.post
    RegexType("put"),
    RegexType("delete"),
    RegexType("patch"),
    RegexType("fetch"),          // global.fetch, node-fetch
    RegexType("curl"),

    // Socket / TCP
    RegexType("connect"),        // net.connect, tls.connect
    RegexType("createConnection"),
    RegexType("send"),           // ws.send, socket.emit, dgram.send
    RegexType("emit"),           // socket.io emit

    // Constructors
    RegexType("ClientRequest"),
    RegexType("OutgoingMessage"),
    RegexType("Agent"),
    RegexType("Socket"),
    RegexType("Stream"),
    RegexType("TLSSocket"),

    // Database Queries (SQL Injection Sinks)
    RegexType("query"),          // db.query, mysql.query, pg.query
    RegexType("execute"),        // connection.execute
    RegexType("insert"),         // collection.insert
    RegexType("save")            // model.save
  )

  /**
   * NHÓM 4: MÃ HÓA & NÉN (Dùng để che giấu Payload/Obfuscation)
   */
  private val DYNAMIC_CODE_EXECUTION_AND_OBFUSCATION: Seq[TypeDefinition] = Seq(
    // Crypto
    RegexType("createCipheriv"),
    RegexType("createCipher"),
    RegexType("publicEncrypt"),
    RegexType("privateEncrypt"),
    RegexType("sign"),
    RegexType("createHmac"),
    RegexType("createHash"),
    RegexType("pbkdf2"),
    RegexType("scrypt"),

    // Compression
    RegexType("deflate"),
    RegexType("gzip"),
    RegexType("brotliCompress"),
    RegexType("createDeflate"),
    RegexType("createGzip"),
    
    // Encoding/Decoding helpers
    RegexType("transcode"),
    RegexType("btoa"),
    RegexType("atob"),
    RegexType("stringify"),      // JSON.stringify, querystring.stringify
    RegexType("escape")
  )

  /**
   * NHÓM 5: DỌN DẸP & ĐIỀU KHIỂN PROCESS (EVASION / DESTRUCTION)
   */
  private val ENVIRONMENT_CLEANUP: Seq[TypeDefinition] = Seq(
    // Deletion
    RegexType("unlink"),         // fs.unlink (Xóa file)
    RegexType("unlinkSync"),
    RegexType("rm"),
    RegexType("rmSync"),
    RegexType("rmdir"),
    RegexType("rmdirSync"),
    RegexType("truncate"),
    RegexType("truncateSync"),
    RegexType("ftruncate"),
    RegexType("ftruncateSync"),

    // Process Control
    RegexType("exit"),
    RegexType("reallyExit"),
    RegexType("kill"),
    RegexType("_kill"),
    RegexType("abort"),
    RegexType("umask"),
    RegexType("setuid"),
    RegexType("setgid"),
    RegexType("seteuid"),
    RegexType("setegid"),
    RegexType("setgroups"),
    RegexType("initgroups"),
    RegexType("chdir"),

    // Debugging / Anti-Analysis
    RegexType("_debugProcess"),
    RegexType("_debugEnd"),
    RegexType("setSourceMapsEnabled"),
    RegexType("_startProfilerIdleNotifier"),
    RegexType("_stopProfilerIdleNotifier"),
    RegexType("unwatchFile")
  )
  
  private val PARALLEL_TASK: Seq[TypeDefinition] = Seq(
    RegexType("nextTick"),
    RegexType("_tickCallback")
  )

  def getAllSinks: Set[TypeDefinition] = {
    (FILE_OPERATIONS ++
      SYSTEM_COMMAND_EXECUTION ++
      NETWORK_COMMUNICATION ++
      DYNAMIC_CODE_EXECUTION_AND_OBFUSCATION ++
      ENVIRONMENT_CLEANUP ++
      PARALLEL_TASK).toSet
  }
}