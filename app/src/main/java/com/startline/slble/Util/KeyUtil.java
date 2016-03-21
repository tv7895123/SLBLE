package com.startline.slble.Util;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by terry on 2015/3/31.
 */
public class KeyUtil
{
	private static final String TAG = "KeyUtil";
	public static final String ANDROID_KEY_STORE = "AndroidKeyStore";
	public static final String AES = "AES";
	public static final String RSA = "RSA";
	public static final String RSA_PUBLIC = "RSA_PUBLIC";
	public static final String RSA_PRIVATE = "RSA_PRIVATE";
	private static final String KEY_STORE_INSTANCE_PKCS12 = "PKCS12";


	//***********************************************************
	// *	AES
	// **********************************************************
	public static byte[] generateAesKey(final int outputKeyLength)
	{
		try
		{
			// Do *not* seed secureRandom! Automatically seeded from system entropy.
			final SecureRandom secureRandom = new SecureRandom();
			final KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);

			keyGenerator.init(outputKeyLength, secureRandom);
			final SecretKey key = keyGenerator.generateKey();

			//return key;
			return key.getEncoded();
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}

		return null;
	}

	public static void generateAesKeyInKeyStore(final Context context,final String alias, final int keyLength )
	{
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.load(null);

			//String alias = "my_key"; // replace as required or get it as a function argument

			final int nBefore = keyStore.size(); // debugging variable to help convince yourself this works

			// Create the keys if necessary
			if (!keyStore.containsAlias(alias))
			{
				final KeyGenerator generator = KeyGenerator.getInstance(AES);

				if(generator != null)
				{
					final String type = keyStore.getType();
					final String provider = keyStore.getProvider().getName();
					generator.init(keyLength);

					final SecretKey secretKey = generator.generateKey();
				}
			}
			final int nAfter = keyStore.size();
			Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);

			keyStore.deleteEntry(alias);
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}
	}

	public static KeyStore.SecretKeyEntry getAesKeyFromKeyStore(final String alias)
	{
		KeyStore.SecretKeyEntry secretKeyEntry = null;
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.load(null);

			// Retrieve the keys
			secretKeyEntry = (KeyStore.SecretKeyEntry)keyStore.getEntry(alias, null);

			final SecretKey secretKey = (SecretKey) secretKeyEntry.getSecretKey();

			Log.v(TAG, "secretKey = " + secretKey.toString());
		}
		catch (Exception e)
		{
		 	Log.e(TAG,e.toString());
		}

		return secretKeyEntry;
	}


	//***********************************************************
	// *	RSA      (Ver 1.)
	// **********************************************************
	public static List<byte[]> generateRsaKeyPair(final Context context,final String alias, final int keyLength )
	{
		try
		{
			final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(keyLength);

			final KeyPair keyPair = keyPairGenerator.genKeyPair();

//			saveRsaKey(context,RSA_PRIVATE + alias, keyPair.getPrivate().getEncoded());
//
//			saveRsaKey(context,RSA_PUBLIC + alias,keyPair.getPublic().getEncoded());

			final List<byte[]> keyList = new ArrayList<>();
			keyList.add(keyPair.getPrivate().getEncoded());
			keyList.add(keyPair.getPublic().getEncoded());

			return keyList;
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}

		return null;
	}

	public static void generateRsaKeyInOs(final Context context,final String alias, final int keyLength )
	{
		long start = System.currentTimeMillis();
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.load(null);

			//String alias = "my_key"; // replace as required or get it as a function argument

			final int nBefore = keyStore.size(); // debugging variable to help convince yourself this works

			// Delete original key if exists
			if (keyStore.containsAlias(alias))
			{
				keyStore.deleteEntry(alias);
			}


			// Create new key
			final Calendar notBefore = Calendar.getInstance();
			final Calendar notAfter = Calendar.getInstance();
			notAfter.add(Calendar.YEAR, 30);
			final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
					.setAlias(alias)
					.setKeyType(RSA)
					.setKeySize(keyLength)
					.setSubject(new X500Principal("CN=startline"))
					.setSerialNumber(BigInteger.ONE)
					.setStartDate(notBefore.getTime())
					.setEndDate(notAfter.getTime())
					.build();

			final KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA, ANDROID_KEY_STORE);
			generator.initialize(spec);
			final String provider = generator.getProvider().getName();
			final String type = generator.getAlgorithm();
			final KeyPair keyPair = generator.generateKeyPair();



			// debug
			final int nAfter = keyStore.size();
			Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}

		Log.d(TAG,"generateRsaKey cost "+ (System.currentTimeMillis()-start));
	}

	public static KeyStore.PrivateKeyEntry getRsaKeyFromKeyStore(final String alias)
	{
		long start = System.currentTimeMillis();
		KeyStore.PrivateKeyEntry privateKeyEntry = null;
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.load(null);

			// Retrieve the keys
			privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);

			final RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
			final RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

			Log.v(TAG, "private key = " + privateKey.toString());
			Log.v(TAG, "public key = " + publicKey.toString());
		}
		catch (Exception e)
		{
		 	Log.e(TAG,e.toString());
		}

		Log.d(TAG,"getRsaKey cost "+ (System.currentTimeMillis()-start));
		return privateKeyEntry;
	}

	public static byte[] getRsaPublicKeyFromKeyStore(final String alias,final int requestLength)
	{
		long start = System.currentTimeMillis();
		byte[] rsaKey = null;
		try
		{
			final KeyStore.PrivateKeyEntry privateKeyEntry = getRsaKeyFromKeyStore(alias);
			final RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();
			final byte[] key = publicKey.getEncoded();
			final int outputKeyLength = key.length< requestLength/8? key.length : requestLength/8;
			final int keyOffset = 32;
			rsaKey = new byte[outputKeyLength];
			for(int i=0;i<outputKeyLength;i++)
			{
				rsaKey[i] = key[keyOffset + i];
			}
		}
		catch (Exception e)
		{
		 	Log.e(TAG,e.toString());
		}
		Log.d(TAG,"getRsaPublicKey cost "+ (System.currentTimeMillis()-start));
		return rsaKey;
	}

	public static byte[] getRsaPrivateKeyFromKeyStore(final String alias,final int requestLength)
	{
		long start = System.currentTimeMillis();
		byte[] rsaKey = null;
		try
		{
			final KeyStore.PrivateKeyEntry privateKeyEntry = getRsaKeyFromKeyStore(alias);
			final RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
			final byte[] key = privateKey.getModulus().toByteArray();
			final int outputKeyLength = key.length< requestLength/8? key.length : requestLength/8;

			rsaKey = new byte[outputKeyLength];
			for(int i=0;i<outputKeyLength;i++)
			{
				rsaKey[i] = key[i];
			}
		}
		catch (Exception e)
		{
		 	Log.e(TAG,e.toString());
		}

		Log.d(TAG,"getRsaPrivateKey cost "+ (System.currentTimeMillis()-start));
		return rsaKey;
	}

	public static boolean saveKeyInKeyStore(final String alias,final SecretKey secretKey)
	{
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.setKeyEntry(alias,secretKey.getEncoded(),null);

			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}

		return false;
	}



	//***********************************************************
	// *	RSA        (Ver 2.)
	// **********************************************************
//	public static void saveRsaKeyInStorage(final Context context,final String tag,final byte[] key)
//	{
//		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
//		final String baseString = Base64.encodeToString(key, Base64.DEFAULT);
//		sharedPreferences.edit().putString(tag, baseString).apply();
//		if(key != null && key.length > 0)
//		{
//			String s = "";
//			for(int i=0;i<key.length;i++)
//			{
//				s = s+ String.format("%X",key[i]) + " ";
//			}
//			Log.d("BLE",String.format("SaveKey : %s",s));
//		}
//
//		Log.d("BLE",String.format("%s : %s",tag,baseString));
//	}
//
//	public static byte[] getRsaKeyFromStorage(final Context context,final String tag,final int requestLength)
//	{
//		try
//		{
//			final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
//			final byte[] tempKey = Base64.decode(sharedPreferences.getString(tag, ""), Base64.DEFAULT);
//
//			// Key index offset, the high bytes of generated RSA public key usually are same
//			// So we give it a offset to refer to lower byte to get dynamic bytes
//			int keyOffset = 0;
//			if(tag.equalsIgnoreCase(RSA_PUBLIC))
//				keyOffset = 32;
//
//			return retrieveKey(tempKey, requestLength, keyOffset);
//		}
//		catch (Exception e)
//		{
//			Log.e(TAG,e.toString());
//		}
//		return null;
//	}
//	public static byte[] retrieveKey(final byte[] tempKey,int requestLength,final int keyOffset)
//	{
//		byte[] key = null;
//		try
//		{
//			requestLength = requestLength / 8;
//			if(requestLength > 0 && requestLength < tempKey.length)
//			{
//				key = new byte[requestLength];
//				for(int i=0;i<requestLength;i++)
//				{
//					key[i] = tempKey[keyOffset + i];
//				}
//			}
//		}
//		catch (Exception e)
//		{
//
//		}
//
//		if(key != null && key.length > 0)
//		{
//			String s = "";
//			for(int i=0;i<key.length;i++)
//			{
//				s = s+ String.format("%X",key[i]) + " ";
//			}
//			Log.d("BLE",String.format("Get Key : %s",s));
//		}
//
//		return key;
//	}



	//***********************************************************
	// *	RSA In Storage       (Ver 3.)
	// **********************************************************
	public static List<Key> generateRsaKey(final String password , final String privatePath, final String publicPath, final int keyLength , final long expiredTime)
	{

		final List<Key> keyList = new ArrayList<>();
		try
		{
			// Generate RSA key pair
			final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keyLength);
			final KeyPair keyPair = kpg.genKeyPair();
			final Key publicKey = keyPair.getPublic();
			final Key privateKey = keyPair.getPrivate();
			keyList.add(privateKey);
			keyList.add(publicKey);

			// Init KeyStore
			final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_INSTANCE_PKCS12);
			keyStore.load(null, null);;

			// Generate a certification
			final CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA","SHA1WithRSA");
			certAndKeyGen.generate(keyLength);
			final X509Certificate cert = certAndKeyGen.getSelfCertificate(new X500Name("CN=ROOT"), expiredTime);
			final X509Certificate[] chain = new X509Certificate[]{cert};

			// Save private key in KeyStore with certification
			keyStore.setKeyEntry("private", privateKey, null, chain);
			keyStore.store(new FileOutputStream(privatePath), password.toCharArray());

			// Save public key in a file
			exportPublicKey(publicKey,publicPath);

			return keyList;
		}
		catch(Exception ex)
		{
			Log.e(TAG,ex.toString());
		}

		return null;
	}

	private static void exportPublicKey(final Key publicKey,final String path)
	{
		try
		{
			final FileOutputStream fileOutputStream = new FileOutputStream(path);
			fileOutputStream.write(publicKey.getEncoded());
			fileOutputStream.flush();
			fileOutputStream.close();
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}
	}

	public static Key loadRsaKey(final String password, final String path) throws Exception
	{
		final String KEY_STORE_INSTANCE_PKCS12 = "PKCS12";
		final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_INSTANCE_PKCS12);
		keyStore.load(new FileInputStream(path), password.toCharArray());

		final Key key = keyStore.getKey("private", null);
		return key;
	}

	public static Key importPublicKey(final String path)
	{
		try
		{
			byte[] temp = new byte[2048];
			FileInputStream fileInputStream = new FileInputStream(path);
			fileInputStream.read(temp);
			fileInputStream.close();

			return getPublicKey(temp);
		}
		catch (Exception e)
		{
		 	Log.e(TAG,e.toString());
		}

		return null;
	}

	/**
	 * Encrypt with public key
	 * data must less than length of public key -11
	 */
	public static byte[] encryptDataWithRsa(final byte[] data, final PublicKey publicKey)
	{
		try
		{
			final Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(data);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Decrypt with private key
	 */
	public static byte[] decryptDataWithRsa(final byte[] encryptedData, final PrivateKey privateKey)
	{
		try
		{
			final Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(encryptedData);
		} catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Convert byte[] to public key for RSA
	 */
	public static PublicKey getPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		final PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	/**
	 * Convert byte[] to private key for RSA
	 */
	public static PrivateKey getPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}

	/**
	 * Convert modulus and publicExponent to public key
	 */
	public static PublicKey getPublicKey(String modulus, String publicExponent) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		final BigInteger bigIntModulus = new BigInteger(modulus);
		final BigInteger bigIntPrivateExponent = new BigInteger(publicExponent);
		final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(bigIntModulus, bigIntPrivateExponent);
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		final PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	/**
	 * Convert modulus and privateExponent to private key
	 */
	public static PrivateKey getPrivateKey(String modulus, String privateExponent) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		final BigInteger bigIntModulus = new BigInteger(modulus);
		final BigInteger bigIntPrivateExponent = new BigInteger(privateExponent);
		final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(bigIntModulus, bigIntPrivateExponent);
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}











	/*
	 * Load the Android KeyStore instance using the the
	 * "AndroidKeyStore" provider to list out what entries are
	 * currently stored.
	 */
	public static Enumeration<String> listEntries()
	{
		Enumeration<String> aliases = null;
		try
		{
			final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
			keyStore.load(null);
			aliases = keyStore.aliases();
		}
		catch (Exception e)
		{
			Log.e(TAG,e.toString());
		}
		return aliases;
	}
}
