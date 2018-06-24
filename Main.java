import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {
	static class ReplyThread extends Thread {
		String name;
		InputStream inStream;
		OutputStream outStream;
		ByteArrayOutputStream packetBuffer = new ByteArrayOutputStream(1024);
		byte[] buf = new byte[1024];
		int cur, len;
		boolean isBlock = false;
		List<Integer> block;
		
		ReplyThread(String name, InputStream inStream, OutputStream outStream) {
			this.name = name;
			this.inStream = inStream;
			this.outStream = outStream;
		}
		
		ReplyThread(String name, InputStream inStream, OutputStream outStream, List<Integer> blockedPacket) {
			this.name = name;
			this.inStream = inStream;
			this.outStream = outStream;
			isBlock = true;
			block = blockedPacket;
		}
		
		byte readByte() throws Exception{
			if(cur == len) {
				len = inStream.read(buf);
				if(len == -1) throw new Exception(name + " Connection Lost");
				//System.out.println(name + " LEN:" + len);
				cur = 0;
			}
			//System.out.println(name + " " + cur);
			packetBuffer.write(buf[cur]);
			return buf[cur++];
		}
		
		int readVarInt() throws Exception{
		    int numRead = 0;
		    int result = 0;
		    byte read;
		    do {
		        read = readByte();
		        int value = (read & 0b01111111);
		        result |= (value << (7 * numRead));

		        numRead++;
		        if (numRead > 5) {
		            throw new RuntimeException("VarInt is too big");
		        }
		    } while ((read & 0b10000000) != 0);

		    return result;
		}
		
		@Override
		public void run() {
			try {
				cur = 0; len = 0;
				
				while(true) {
					packetBuffer.reset();
					
					int packetLength = readVarInt();
					
					while(packetLength-- != 0) readByte();
					packetBuffer.writeTo(outStream);
					outStream.flush();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		}
	}
	
	
	public static void writeVarInt(int value, OutputStream out) throws IOException {
	    do {
	        byte temp = (byte)(value & 0b01111111);
	        // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
	        value >>>= 7;
	        if (value != 0) {
	            temp |= 0b10000000;
	        }
	        out.write(temp);;
	    } while (value != 0);
	}
	
	
	public static void main(String[] args) throws Exception{
		Scanner scan = new Scanner(System.in);
		ServerSocket servSocket = new ServerSocket(2333, 5, InetAddress.getLoopbackAddress());
		while(true) {
			Socket Csocket = servSocket.accept();
			Socket Ssocket = new Socket("127.0.0.1", 233);
			InputStream CinStream = Csocket.getInputStream();
			InputStream SinStream = Ssocket.getInputStream();
			OutputStream CoutStream = Csocket.getOutputStream();
			OutputStream SoutStream = Ssocket.getOutputStream();
			
			List<Integer> block = new LinkedList();
			block.add(0x01);//Tab-Conplete
			
			ReplyThread replyA = new ReplyThread("CtoS", CinStream, SoutStream);
			ReplyThread replyB = new ReplyThread("StoC",SinStream, CoutStream);
			replyA.start(); replyB.start();
			
			System.out.println("连接准备好了 回车键开始");
			scan.nextLine();
			//ByteArrayOutputStream packet = new ByteArrayOutputStream(1024);
			byte[] packet = new byte[] {0x06, 0x00, 0x01, 0x01, 0x2F, 0x00, 0x00};
			while(true) {
				SoutStream.write(packet);
			}
		}
		
		/**/
	}
}
