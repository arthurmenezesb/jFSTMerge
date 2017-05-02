package br.ufpe.cin.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import br.ufpe.cin.exceptions.CryptoException;

public class CryptoUtils {


	private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final SecretKey SECRETKEY = CryptoKey.getKey(); 

	public static void encrypt(File inputFile, File outputFile) throws CryptoException
	{
		doCrypto(Cipher.ENCRYPT_MODE, inputFile, outputFile);
	}

	public static void decrypt(File inputFile, File outputFile)
			throws CryptoException {
		doCrypto(Cipher.DECRYPT_MODE,inputFile, outputFile);
	}

	private static void doCrypto(int cipherMode, File input, File output) throws CryptoException
	{
		try
		{

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			byte[] iv = new byte[cipher.getBlockSize()];

			IvParameterSpec ivParams = new IvParameterSpec(iv);
			cipher.init(cipherMode, SECRETKEY, ivParams);

			FileInputStream inputStream = new FileInputStream(input);
			byte [] inputBytes = new byte[(int) input.length()];
			inputStream.read(inputBytes);

			byte [] outputBytes = cipher.doFinal(inputBytes);

			FileOutputStream outputStream = new FileOutputStream(output,false);
			outputStream.write(outputBytes);

			inputStream.close();
			outputStream.close();

		}
		catch (NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidKeyException | BadPaddingException
				| IllegalBlockSizeException | IOException | InvalidAlgorithmParameterException ex) 
		{
			try 
			{
				Files.delete(Paths.get(input.getAbsolutePath()));
				ex.printStackTrace();
			} 
			catch (IOException e) 
			{
				throw new CryptoException("Error deleting the invalid encrypted file", e);
			}
			throw new CryptoException("Error encrypting/decrypting file", ex);
		}
	}

	/*	public static void main(String[] args) {
		File f = new File("C:\\Users\\Guilherme Cavalcanti\\.jfstmerge\\jfstmerge.files");
		try {
			new CryptoUtils().decrypt(f, f);
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

}
