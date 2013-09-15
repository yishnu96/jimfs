/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jimfs.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;

/**
 * @author Colin Decker
 */
final class JimfsFileSystem extends FileSystem {

  private final JimfsFileSystemProvider provider;
  private final URI uri;

  private final FileSystemService service;

  JimfsFileSystem(JimfsFileSystemProvider provider, URI uri, FileSystemService service) {
    this.provider = checkNotNull(provider);
    this.uri = checkNotNull(uri);
    this.service = checkNotNull(service);
  }

  @Override
  public JimfsFileSystemProvider provider() {
    return provider;
  }

  /**
   * Returns the file system service.
   */
  public FileSystemService service() {
    return service;
  }

  @Override
  public String getSeparator() {
    return service.paths().getSeparator();
  }

  @Override
  public ImmutableSet<Path> getRootDirectories() {
    ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
    DirectoryTable superRootTable = service.getSuperRoot().content();
    for (Name name : superRootTable.snapshot()) {
      builder.add(service.paths().createRoot(name));
    }
    return builder.build();
  }

  /**
   * Returns the working directory path for this file system.
   */
  public JimfsPath getWorkingDirectory() {
    return service.getWorkingDirectoryPath();
  }

  @Override
  public ImmutableSet<FileStore> getFileStores() {
    return ImmutableSet.<FileStore>of(service.fileStore());
  }

  @Override
  public ImmutableSet<String> supportedFileAttributeViews() {
    return service.fileStore().supportedFileAttributeViews();
  }

  @Override
  public JimfsPath getPath(String first, String... more) {
    return service.paths().parsePath(first, more);
  }

  /**
   * Gets the URI of the given path in this file system.
   */
  public URI getUri(JimfsPath path) {
    checkArgument(path.getFileSystem() == this);

    String pathString = path.toAbsolutePath().toString();
    if (!pathString.startsWith("/")) {
      pathString = "/" + pathString;
    }

    return URI.create(uri.toString() + pathString);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    return service.paths().createPathMatcher(syntaxAndPattern);
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    return null;
  }

  @Override
  public WatchService newWatchService() throws IOException {
    return new PollingWatchService(service);
  }

  /**
   * Returns {@code false}; currently, cannot create a read-only file system.
   *
   * @return {@code false}, always
   */
  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public void close() throws IOException {
    service.resourceManager().close();
  }
}
