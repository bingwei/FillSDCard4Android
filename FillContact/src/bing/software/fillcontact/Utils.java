package bing.software.fillcontact;

import java.util.Random;


public class Utils {
	public Utils(){
		
	}
	public static String getRandomString(int length) { //length表示生成字符串的长度
	    String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.";   //生成字符串从此序列中取
	    int baseLength = base.length();
	    Random random = new Random();   
	    StringBuffer sb = new StringBuffer();
	    if(length < 1){
	    	throw new ArrayIndexOutOfBoundsException();
	    }else{
		    for (int i = 0; i < length; i++) {   
		        int number = random.nextInt(baseLength);   
		        sb.append(base.charAt(number));   
		    }   
		    return sb.toString();   
	    }
	 } 
	
	public static String getRandomNumber(int length) { //length表示生成字符串的长度
		Random ran = new Random();
		StringBuffer strb = new StringBuffer();
		int num1 = 0;
		int digit = (int)Math.pow(10, length % 8 -1);
		if(length < 1){
	    	throw new ArrayIndexOutOfBoundsException();
	    }else{
			for (int i = 0; i < length / 8; i++) {// 这里是产生9位的64/8=8次，
				while (true) {
					num1 = ran.nextInt((int)(99999999));
					System.out.println(num1);
					if (num1 > 10000000) {
	
						strb.append(num1);
						break;
					}
				}
			}
			if(length % 8 != 0){
				System.out.println("=====================>left: " + length % 8);
				while (true) {
					num1 = ran.nextInt((int)(9.9999999 * digit));
					System.out.println(num1);
					if (num1 > digit) {
						
						strb.append(num1);
						break;
					}
				}
			}
			return String.valueOf(strb);
	    }
		
	}
	
	public static String getRandomEmail() { 
		int nameLength = 10;
		int companyLength = 6;
	    String base = "abcdefghijklmnopqrstuvwxyz";   //生成字符串从此序列中取
	    String at = "@";
	    String com = ".com";
	    int baseLength = base.length();
	    Random random = new Random();   
	    StringBuffer sb = new StringBuffer();
	    
	    for (int i = 0; i < nameLength; i++) {   
	        int number = random.nextInt(baseLength);   
	        sb.append(base.charAt(number));   
	    }
	    sb.append(at);
	    for (int i = 0; i < companyLength; i++) {   
	        int number = random.nextInt(baseLength);   
	        sb.append(base.charAt(number));   
	    }
	    sb.append(com);
	    return sb.toString();   
    }

}
