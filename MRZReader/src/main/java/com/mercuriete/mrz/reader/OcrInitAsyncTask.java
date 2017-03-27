/*
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mercuriete.mrz.reader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;


import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Installs the language data required for OCR, and initializes the OCR engine using a background 
 * thread.
 */
final class OcrInitAsyncTask extends AsyncTask<String, String, Boolean> {
  private static final String TAG = OcrInitAsyncTask.class.getSimpleName();

  /** Suffixes of required data files for Cube. */
  private static final String[] CUBE_DATA_FILES = {
    ".cube.bigrams",
    ".cube.fold", 
    ".cube.lm", 
    ".cube.nn", 
    ".cube.params", 
    //".cube.size", // This file is not available for Hindi
    ".cube.word-freq", 
    ".tesseract_cube.nn", 
    ".traineddata"
  };

  private CaptureActivity activity;
  private Context context;
  private TessBaseAPI baseApi;
  private ProgressDialog dialog;
  private ProgressDialog indeterminateDialog;
  private final String languageCode;
  private String languageName;
  private int ocrEngineMode;

  /**
   * AsyncTask to asynchronously download data and initialize Tesseract.
   * 
   * @param activity
   *          The calling activity
   * @param baseApi
   *          API to the OCR engine
   * @param dialog
   *          Dialog box with thermometer progress indicator
   * @param indeterminateDialog
   *          Dialog box with indeterminate progress indicator
   * @param languageCode
   *          ISO 639-2 OCR language code
   * @param languageName
   *          Name of the OCR language, for example, "English"
   * @param ocrEngineMode
   *          Whether to use Tesseract, Cube, or both
   */
  OcrInitAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, ProgressDialog dialog, 
      ProgressDialog indeterminateDialog, String languageCode, String languageName, 
      int ocrEngineMode) {
    this.activity = activity;
    this.context = activity.getBaseContext();
    this.baseApi = baseApi;
    this.dialog = dialog;
    this.indeterminateDialog = indeterminateDialog;
    this.languageCode = languageCode;
    this.languageName = languageName;
    this.ocrEngineMode = ocrEngineMode;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    dialog.setTitle("Please wait");
    dialog.setMessage("Checking for data installation...");
    dialog.setIndeterminate(false);
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setCancelable(false);
    dialog.show();
    activity.setButtonVisibility(false);
  }

  /**
   * In background thread, perform required setup, and request initialization of
   * the OCR engine.
   * 
   * @param params
   *          [0] Pathname for the directory for storing language data files to the SD card
   */
  protected Boolean doInBackground(String... params) {
    // Check whether we need Cube data or Tesseract data.
    // Example Cube data filename: "tesseract-ocr-3.01.eng.tar"
    // Example Tesseract data filename: "eng.traineddata"
    String destinationFilenameBase = languageCode + ".traineddata";
    boolean isCubeSupported = false;
    for (String s : CaptureActivity.CUBE_SUPPORTED_LANGUAGES) {
      if (s.equals(languageCode)) {
        isCubeSupported = true;   
      }
    }

    // Check for, and create if necessary, folder to hold model data
    String destinationDirBase = params[0]; // The storage directory, minus the
                                           // "tessdata" subdirectory
    File tessdataDir = new File(destinationDirBase + File.separator + "tessdata");
    if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
      Log.e(TAG, "Couldn't make directory " + tessdataDir);
      return false;
    }

    // Create a reference to the file to save the download in
    File downloadFile = new File(tessdataDir, destinationFilenameBase);

    // Check if an incomplete download is present. If a *.download file is there, delete it and
    // any (possibly half-unzipped) Tesseract and Cube data files that may be there.
    File tesseractTestFile = new File(tessdataDir, languageCode + ".traineddata");

    // Check whether all Cube data files have already been installed
    boolean isAllCubeDataInstalled = false;
    if (isCubeSupported) {
      boolean isAFileMissing = false;
      File dataFile;
      for (String s : CUBE_DATA_FILES) {
        dataFile = new File(tessdataDir.toString() + File.separator + languageCode + s);
        if (!dataFile.exists()) {
          isAFileMissing = true;
        }
      }
      isAllCubeDataInstalled = !isAFileMissing;
    }

    // If language data files are not present, install them
    boolean installSuccess = false;
    if (!tesseractTestFile.exists()
        || (isCubeSupported && !isAllCubeDataInstalled)) {
      Log.d(TAG, "Language data for " + languageCode + " not found in " + tessdataDir.toString());
      deleteCubeDataFiles(tessdataDir);

      // Check assets for language data to install. If not present, download from Internet
      try {
        Log.d(TAG, "Checking for language data (" + destinationFilenameBase
            + ") in application assets...");
        // Check for a file like "eng.traineddata.zip" or "tesseract-ocr-3.01.eng.tar.zip"
        installSuccess = installFromAssets(destinationFilenameBase, tessdataDir,
            downloadFile);
      } catch (IOException e) {
        Log.e(TAG, "IOException", e);
      } catch (Exception e) {
        Log.e(TAG, "Got exception", e);
      }


    } else {
      Log.d(TAG, "Language data for " + languageCode + " already installed in " 
          + tessdataDir.toString());
      installSuccess = true;
    }

    // Dismiss the progress dialog box, revealing the indeterminate dialog box behind it
    try {
      dialog.dismiss();
    } catch (IllegalArgumentException e) {
      // Catch "View not attached to window manager" error, and continue
    }

      File f = new File(this.activity.getCacheDir()+"/tessdata/eng.traineddata");
      File folder = new File(this.activity.getCacheDir()+"/tessdata");
      if (!f.exists()) try {

          if (!f.exists() && !folder.mkdirs()) {
              Log.e(TAG, "Couldn't make directory " + tessdataDir);
              return false;
          }

          InputStream is = this.activity.getAssets().open("tessdata/eng.traineddata");
          FileOutputStream fos = new FileOutputStream(f);
          copyFile(is,fos);
          fos.close();
          is.close();
      } catch (Exception e) { throw new RuntimeException(e); }


      // Initialize the OCR engine
    if (baseApi.init(this.activity.getCacheDir() + File.separator, languageCode, ocrEngineMode)) {
      return true;
    }
    return false;
  }

  /**
   * Delete any existing data files for Cube that are present in the given directory. Files may be 
   * partially uncompressed files left over from a failed install, or pre-v3.01 traineddata files.
   * 
   * @param tessdataDir
   *          Directory to delete the files from
   */
  private void deleteCubeDataFiles(File tessdataDir) {
    File badFile;
    for (String s : CUBE_DATA_FILES) {
      badFile = new File(tessdataDir.toString() + File.separator + languageCode + s);
      if (badFile.exists()) {
        Log.d(TAG, "Deleting existing file " + badFile.toString());
        badFile.delete();
      }
      badFile = new File(tessdataDir.toString() + File.separator + "tesseract-ocr-3.01." 
          + languageCode + ".tar");
      if (badFile.exists()) {
        Log.d(TAG, "Deleting existing file " + badFile.toString());
        badFile.delete();
      }
    }
  }

  /**
   * Unzips the given Gzipped file to the given destination, and deletes the
   * gzipped file.
   * 
   * @param zippedFile
   *          The gzipped file to be uncompressed
   * @param outFilePath
   *          File to unzip to, including path
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void gunzip(File zippedFile, File outFilePath)
      throws FileNotFoundException, IOException {
    int uncompressedFileSize = getGzipSizeUncompressed(zippedFile);
    Integer percentComplete;
    int percentCompleteLast = 0;
    int unzippedBytes = 0;
    final Integer progressMin = 0;
    int progressMax = 100 - progressMin;
    publishProgress("Uncompressing data for " + languageName + "...",
        progressMin.toString());

    // If the file is a tar file, just show progress to 50%
    String extension = zippedFile.toString().substring(
        zippedFile.toString().length() - 16);
    if (extension.equals(".tar.gz.download")) {
      progressMax = 50;
    }
    GZIPInputStream gzipInputStream = new GZIPInputStream(
        new BufferedInputStream(new FileInputStream(zippedFile)));
    OutputStream outputStream = new FileOutputStream(outFilePath);
    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
        outputStream);

    final int BUFFER = 8192;
    byte[] data = new byte[BUFFER];
    int len;
    while ((len = gzipInputStream.read(data, 0, BUFFER)) > 0) {
      bufferedOutputStream.write(data, 0, len);
      unzippedBytes += len;
      percentComplete = (int) ((unzippedBytes / (float) uncompressedFileSize) * progressMax)
          + progressMin;

      if (percentComplete > percentCompleteLast) {
        publishProgress("Uncompressing data for " + languageName
            + "...", percentComplete.toString());
        percentCompleteLast = percentComplete;
      }
    }
    gzipInputStream.close();
    bufferedOutputStream.flush();
    bufferedOutputStream.close();

    if (zippedFile.exists()) {
      zippedFile.delete();
    }
  }

  /**
   * Returns the uncompressed size for a Gzipped file.
   * 
   * @param zipFile
   *          Gzipped file to get the size for
   * @return Size when uncompressed, in bytes
   * @throws IOException
   */
  private int getGzipSizeUncompressed(File zipFile) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(zipFile, "r");
    raf.seek(raf.length() - 4);
    int b4 = raf.read();
    int b3 = raf.read();
    int b2 = raf.read();
    int b1 = raf.read();
    raf.close();
    return (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
  }

  /**
   * Untar the contents of a tar file into the given directory, ignoring the
   * relative pathname in the tar file, and delete the tar file.
   * 
   * Uses jtar: http://code.google.com/p/jtar/
   * 
   * @param tarFile
   *          The tar file to be untarred
   * @param destinationDir
   *          The directory to untar into
   * @throws IOException
   */
  private void untar(File tarFile, File destinationDir) throws IOException {
    Log.d(TAG, "Untarring...");
    final int uncompressedSize = getTarSizeUncompressed(tarFile);
    Integer percentComplete;
    int percentCompleteLast = 0;
    int unzippedBytes = 0;
    final Integer progressMin = 50;
    final int progressMax = 100 - progressMin;
    publishProgress("Uncompressing data for " + languageName + "...",
        progressMin.toString());

    if (tarFile.exists()) {
      tarFile.delete();
    }
  }
  
  /**
   * Return the uncompressed size for a Tar file.
   * 
   * @param tarFile
   *          The Tarred file
   * @return Size when uncompressed, in bytes
   * @throws IOException
   */
  private int getTarSizeUncompressed(File tarFile) throws IOException {
    int size = 0;
    return size;
  }

  /**
   * Install a file from application assets to device external storage.
   * 
   * @param sourceFilename
   *          File in assets to install
   * @param modelRoot
   *          Directory on SD card to install the file to
   * @param destinationFile
   *          File name for destination, excluding path
   * @return True if installZipFromAssets returns true
   * @throws IOException
   */
  private boolean installFromAssets(String sourceFilename, File modelRoot,
      File destinationFile) throws IOException {
    String extension = sourceFilename.substring(sourceFilename.lastIndexOf('.'), 
        sourceFilename.length());
    //try {
      if (extension.equals(".traineddata")) {
        return installTrainedFromAssets(sourceFilename, modelRoot, destinationFile);
      } else {
        throw new IllegalArgumentException("Extension " + extension
            + " is unsupported.");
      }

  }
  private boolean installTrainedFromAssets(String sourceFilename, File modelRoot, File destinationFile) {
    Log.w("CACA",sourceFilename);
    Log.w("CACA", modelRoot.getAbsolutePath());
    Log.w("CACA",destinationFile.getAbsolutePath());
      try {
          InputStream in = new FileInputStream(new File(modelRoot ,sourceFilename));
          OutputStream out = new FileOutputStream(destinationFile);

          return copyFile(in,out);
      } catch (FileNotFoundException e) {
          e.printStackTrace();
      } catch (IOException e) {
          e.printStackTrace();
      }
      return false;
  }

  /**
   * Update the dialog box with the latest incremental progress.
   * 
   * @param message
   *          [0] Text to be displayed
   * @param message
   *          [1] Numeric value for the progress
   */
  @Override
  protected void onProgressUpdate(String... message) {
    super.onProgressUpdate(message);
    int percentComplete = 0;

    percentComplete = Integer.parseInt(message[1]);
    dialog.setMessage(message[0]);
    dialog.setProgress(percentComplete);
    dialog.show();
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
    
    try {
      indeterminateDialog.dismiss();
    } catch (IllegalArgumentException e) {
      // Catch "View not attached to window manager" error, and continue
    }

    if (result) {
      // Restart recognition
      activity.resumeOCR();
      activity.showLanguageName();
    } else {
      activity.showErrorMessage("Error", "Network is unreachable - cannot download language data. "
          + "Please enable network access and restart this app.");
    }
  }

  private boolean copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while((read = in.read(buffer)) != -1){
      out.write(buffer, 0, read);
    }
    return true;
  }
}