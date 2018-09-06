package sftp;
 
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Vector;
 
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
/**
 * 
 * @ClassName: SFTPUtil
 * @Description: sftp连接工具类
 * @date 2017年5月22日 下午11:17:21
 * @version 1.0.0
 */
public class SFTPUtil {
	private transient Logger log = LoggerFactory.getLogger(this.getClass());
    
    private ChannelSftp sftp;
      
    private Session session;
    /** FTP 登录用户名*/  
    private String username;
    /** FTP 登录密码*/  
    private String password;
    /** 私钥 */  
    private String privateKey;
    /** FTP 服务器地址IP地址*/  
    private String host;
    /** FTP 端口*/
    private int port;
      
  
    /** 
     * 构造基于密码认证的sftp对象 
     * @param userName 
     * @param password 
     * @param host 
     * @param port 
     */  
    public SFTPUtil(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }
  
    /** 
     * 构造基于秘钥认证的sftp对象
     * @param userName
     * @param host
     * @param port
     * @param privateKey
     */
    public SFTPUtil(String username, String host, int port, String privateKey) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
    }
  
    public SFTPUtil(){}
  
    /**
     * 连接sftp服务器
     *
     * @throws Exception 
     */
    public void login(){
        try {
            JSch jsch = new JSch();
            if (privateKey != null) {
                jsch.addIdentity(privateKey);// 设置私钥
                log.info("sftp connect,path of private key file：{}" , privateKey);
            }
            log.info("sftp connect by host:{} username:{}",host,username);
  
            session = jsch.getSession(username, host, port);
            log.info("Session is build");
            if (password != null) {
                session.setPassword(password);  
            }
            Properties config = new Properties();
            
            
            config.put("StrictHostKeyChecking", "no");
              
            session.setConfig(config);
            session.connect();
            log.info("Session is connected");
            
            Channel channel = session.openChannel("sftp");
            channel.connect();
            log.info("channel is connected");
  
            sftp = (ChannelSftp) channel;
            log.info(String.format("sftp server host:[%s] port:[%s] is connect successfull", host, port));
        } catch (JSchException e) {
            log.error("Cannot connect to specified sftp server : {}:{} \n Exception message is: {}", new Object[]{host, port, e.getMessage()});  
        }
    }  
  
    /**
     * 关闭连接 server 
     */
    public void logout(){
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
                log.info("sftp is closed already");
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
                log.info("sshSession is closed already");
            }
        }
    }
  
    /** 
     * 将输入流的数据上传到sftp作为文件 
     *  
     * @param directory 
     *            上传到该目录 
     * @param sftpFileName 
     *            sftp端文件名 
     * @param in 
     *            输入流 
     * @throws SftpException  
     * @throws Exception 
     */  
    public void upload(String directory, String sftpFileName, InputStream input) throws SftpException{
        try {  
            sftp.cd(directory);
        } catch (SftpException e) {
            log.warn("directory is not exist");
            sftp.mkdir(directory);
            sftp.cd(directory);
        }
        sftp.put(input, sftpFileName);
        log.info("file:{} is upload successful" , sftpFileName);
    }
  
    /** 
     * 上传单个文件
     *
     * @param directory 
     *            上传到sftp目录 
     * @param uploadFile
     *            要上传的文件,包括路径 
     * @throws FileNotFoundException
     * @throws SftpException
     * @throws Exception
     */
    public void upload(String directory, String uploadFile) throws FileNotFoundException, SftpException{
        File file = new File(uploadFile);
        upload(directory, file.getName(), new FileInputStream(file));
    }
  
    /**
     * 将byte[]上传到sftp，作为文件。注意:从String生成byte[]是，要指定字符集。
     * 
     * @param directory
     *            上传到sftp目录
     * @param sftpFileName
     *            文件在sftp端的命名
     * @param byteArr
     *            要上传的字节数组
     * @throws SftpException
     * @throws Exception
     */
    public void upload(String directory, String sftpFileName, byte[] byteArr) throws SftpException{
        upload(directory, sftpFileName, new ByteArrayInputStream(byteArr));
    }
  
    /** 
     * 将字符串按照指定的字符编码上传到sftp
     *  
     * @param directory
     *            上传到sftp目录
     * @param sftpFileName
     *            文件在sftp端的命名
     * @param dataStr
     *            待上传的数据
     * @param charsetName
     *            sftp上的文件，按该字符编码保存
     * @throws UnsupportedEncodingException
     * @throws SftpException
     * @throws Exception
     */
    public void upload(String directory, String sftpFileName, String dataStr, String charsetName) throws UnsupportedEncodingException, SftpException{  
        upload(directory, sftpFileName, new ByteArrayInputStream(dataStr.getBytes(charsetName)));  
    }
  
    /**
     * 下载文件 
     *
     * @param directory
     *            下载目录 
     * @param downloadFile
     *            下载的文件
     * @param saveFile
     *            存在本地的路径
     * @throws SftpException
     * @throws FileNotFoundException
     * @throws Exception
     */  
    public void download(String directory, String downloadFile, String saveFile) throws SftpException, FileNotFoundException{
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        File file = new File(saveFile);
        sftp.get(downloadFile, new FileOutputStream(file));
        log.info("file:{} is download successful" , downloadFile);
    }
    /** 
     * 下载文件
     * @param directory 下载目录
     * @param downloadFile 下载的文件名
     * @return 字节数组
     * @throws SftpException
     * @throws IOException
     * @throws Exception
     */
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException{
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        InputStream is = sftp.get(downloadFile);
        
        byte[] fileData = IOUtils.toByteArray(is);
        
        log.info("file:{} is download successful" , downloadFile);
        return fileData;
    }
  
    /**
     * 删除文件
     *  
     * @param directory
     *            要删除文件所在目录
     * @param deleteFile
     *            要删除的文件
     * @throws SftpException
     * @throws Exception
     */
    public void delete(String directory, String deleteFile) throws SftpException{
        sftp.cd(directory);
        sftp.rm(deleteFile);
    }
  
    /**
     * 列出目录下的文件
     * 
     * @param directory
     *            要列出的目录
     * @param sftp
     * @return
     * @throws SftpException
     */
    public Vector<?> listFiles(String directory) throws SftpException {
        return sftp.ls(directory);
    }
    
    public static void main(String[] args) throws SftpException, IOException {
//       test45();
//       test146();
       test173();
    }
    
    /**
     * 这个是本地虚拟机的，在实验的时候需要关闭防火墙
     * @throws FileNotFoundException
     * @throws SftpException
     */
    public static void test146() throws FileNotFoundException, SftpException {
    	 SFTPUtil sftp = new SFTPUtil("root", "881213", "192.168.150.146", 22);
         sftp.login();
         File file = new File("D:\\upload\\index.html");
         InputStream is = new FileInputStream(file);
         sftp.upload("/home/yejing", "test_sftp_upload.csv", is);
         sftp.logout();
    }
    
    public static void test45() throws FileNotFoundException, SftpException {
//    	SFTPUtil sftp = new SFTPUtil("root", "SogalV7IN", "10.10.18.45", 22);
    	// 注意这个地方账号和文件的关系
    	// 此账号一定要有足够的权限去访问目标文件目录
    	// 这里merry账号可以访问/home/merry目录
    	// root是超级管理员权限，可以访问所有的目录，也是可以的
    	SFTPUtil sftp = new SFTPUtil("merry", "123456", "10.10.18.45", 22);
        sftp.login();
        File file = new File("D:\\upload\\index.html");
        InputStream is = new FileInputStream(file);
        sftp.upload("/home/merry", "test_sftp_upload.csv", is);
        sftp.logout();
    }
    
    /**
     * 刚开始测试没有通过
     * 由于被防火墙给挡住了
     * 配置好后再试试
     * @throws FileNotFoundException
     * @throws SftpException
     */
    public static void test173() throws FileNotFoundException, SftpException {
    	SFTPUtil sftp = new SFTPUtil("merry", "123456", "10.10.10.173", 22);
        sftp.login();
        File file = new File("D:\\upload\\index.html");
        InputStream is = new FileInputStream(file);
        sftp.upload("/home/merry", "test_sftp_upload.csv", is);
        sftp.logout();
    }
}
