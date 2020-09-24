/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.mapper.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.mapstruct.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TraineeDetailsUtil {

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Id {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Title {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Forenames {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface KnownAs {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Surname {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface MaidenName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface TelephoneNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface MobileNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Email {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address1 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address2 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address3 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address4 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PostCode {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface DateOfBirth {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Gender {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GdcNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GdcStatus {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GmcNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GmcStatus {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Owner {

  }

  @Id
  public String id(Map<String, String> data) {
    return data.get("id");
  }

  @Title
  public String title(Map<String, String> data) {
    return data.get("title");
  }

  @Forenames
  public String forenames(Map<String, String> data) {
    return data.get("forenames");
  }

  @KnownAs
  public String knownAs(Map<String, String> data) {
    return data.get("knownAs");
  }

  @Surname
  public String surname(Map<String, String> data) {
    return data.get("surname");
  }

  @MaidenName
  public String maidenName(Map<String, String> data) {
    return data.get("maidenName");
  }

  @TelephoneNumber
  public String telephoneNumber(Map<String, String> data) {
    return data.get("telephoneNumber");
  }

  @MobileNumber
  public String mobileNumber(Map<String, String> data) {
    return data.get("mobileNumber");
  }

  @Email
  public String email(Map<String, String> data) {
    return data.get("email");
  }

  @Address1
  public String address1(Map<String, String> data) {
    return data.get("address1");
  }

  @Address2
  public String address2(Map<String, String> data) {
    return data.get("address2");
  }

  @Address3
  public String address3(Map<String, String> data) {
    return data.get("address3");
  }

  @Address4
  public String address4(Map<String, String> data) {
    return data.get("address4");
  }

  @PostCode
  public String postCode(Map<String, String> data) {
    return data.get("postCode");
  }

  @DateOfBirth
  public String dateOfBirth(Map<String, String> data) {
    return data.get("dateOfBirth");
  }

  @Gender
  public String gender(Map<String, String> data) {
    return data.get("gender");
  }

  @GdcNumber
  public String gdcNumber(Map<String, String> data) {
    return data.get("gdcNumber");
  }

  @GdcStatus
  public String gdcStatus(Map<String, String> data) {
    return data.get("gdcStatus");
  }

  @GmcNumber
  public String gmcNumber(Map<String, String> data) {
    return data.get("gmcNumber");
  }

  @GmcStatus
  public String gmcStatus(Map<String, String> data) {
    return data.get("gmcStatus");
  }

  @Owner
  public String owner(Map<String, String> data) {
    return data.get("owner");
  }
}
