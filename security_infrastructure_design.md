# Security Infrastructure Design Document
**Widget World Inc. - Security Consultant Report**

## Executive Summary

Alright, so I've been hired to help Widget World Inc. get their security sorted out. They're a small but growing online retailer with 50 employees, and honestly, they need some serious help with their security setup. Since they handle customer payment data, we really can't mess around here - one breach could basically destroy their business.

## Organization Overview

**Company:** Widget World Inc.  
**Size:** 50 employees, single office location  
**Business:** Online retailer specializing in handcrafted widgets  
**Key Concern:** Customer payment data protection

The company is growing fast and they've realized they need proper security measures before they get too big to implement them easily. Smart move, honestly.

## Current Requirements Analysis

Based on what they've told me, here's what they absolutely need:

- External website for customers to browse and buy widgets
- Internal website for employees 
- Remote access for their engineering team
- Basic firewall protection
- Wireless network in the office
- Secure laptop configurations
- Extra protection for customer payment data

## Security Infrastructure Design

### 1. Authentication System

**For External Website (Customers):**
- Multi-factor authentication for customer accounts
- Strong password requirements (12+ characters, complexity rules)
- Account lockout after 5 failed attempts
- Password reset via email verification
- Session timeout after 30 minutes of inactivity

**For Internal Systems (Employees):**
- Single Sign-On (SSO) using Azure AD or similar
- Role-based access control (RBAC)
- Mandatory MFA for all employees
- Quarterly password changes for admin accounts
- Privileged access management for IT staff

### 2. External Website Security

This is where customers will be entering their credit card info, so we need to be extra careful:

- SSL/TLS encryption (minimum TLS 1.3)
- Web Application Firewall (WAF) to block malicious traffic
- Regular security scanning and penetration testing
- PCI DSS compliance for payment processing
- Content Security Policy (CSP) headers
- Rate limiting to prevent brute force attacks
- Regular security updates and patches

### 3. Internal Website Security

For the employee intranet:

- VPN-only access when working remotely
- Network segmentation from public-facing systems
- Regular access reviews and user provisioning/deprovisioning
- Encrypted communications
- Audit logging for all access attempts
- Document management with version control

### 4. Remote Access Solution

Since the engineers need to work from home sometimes:

- Corporate VPN with AES-256 encryption
- Certificate-based authentication
- Split tunneling disabled (all traffic through VPN)
- Network Access Control (NAC) for device compliance
- Remote desktop protocols with MFA
- Monitoring of all remote sessions

### 5. Firewall and Network Rules

**Basic Firewall Configuration:**
- Default deny-all policy
- Allow HTTPS (443) and HTTP (80) for web traffic
- SSH (22) restricted to admin IPs only
- Block unnecessary ports and services
- Regular rule reviews and cleanup
- Intrusion Detection System (IDS) integration

**Network Segmentation:**
- Separate VLANs for different functions
- Guest network isolated from corporate network
- DMZ for public-facing servers
- Database servers on isolated subnet

### 6. Wireless Security

For the office WiFi:

- WPA3 encryption (WPA2 as fallback)
- Strong pre-shared keys changed quarterly
- Hidden SSID for corporate network
- Guest network with captive portal
- MAC address filtering for corporate devices
- Regular monitoring for rogue access points

### 7. VLAN Configuration Recommendations

- **VLAN 10:** Management network (switches, routers, APs)
- **VLAN 20:** Corporate workstations and laptops
- **VLAN 30:** Servers and databases
- **VLAN 40:** Guest/visitor access
- **VLAN 50:** IoT devices and printers
- **VLAN 99:** DMZ for public-facing services

### 8. Laptop Security Configuration

For all company laptops:

- Full disk encryption (BitLocker or FileVault)
- Automatic screen lock after 5 minutes
- Anti-malware software with real-time protection
- Automatic security updates enabled
- VPN client pre-configured
- Local firewall enabled
- USB port restrictions
- Remote wipe capability for lost devices

### 9. Application Policy Recommendations

**Software Installation:**
- Admin approval required for new software
- Whitelist of approved business applications
- Regular software inventory and license management
- Automatic updates for critical security patches

**Data Handling:**
- Classification system (Public, Internal, Confidential, Restricted)
- Data Loss Prevention (DLP) tools
- Email encryption for sensitive information
- Secure file sharing solutions only

### 10. Security and Privacy Policy Recommendations

**Employee Security Training:**
- Monthly security awareness training
- Phishing simulation exercises
- Social engineering awareness
- Incident reporting procedures

**Privacy Policies:**
- GDPR compliance for international customers
- Clear data retention policies
- Customer consent management
- Regular privacy impact assessments

### 11. Intrusion Detection for Customer Data Protection

This is super important since they handle payment information:

- Security Information and Event Management (SIEM) system
- Real-time monitoring of database access
- Behavioral analytics for unusual activity
- Automated incident response procedures
- Regular vulnerability assessments
- Penetration testing twice yearly
- 24/7 security operations center (SOC) or managed service

## Implementation Timeline

**Phase 1 (Month 1):** Basic firewall, wireless security, laptop encryption  
**Phase 2 (Month 2):** VPN setup, internal authentication system  
**Phase 3 (Month 3):** External website security hardening, WAF deployment  
**Phase 4 (Month 4):** SIEM implementation, intrusion detection  
**Phase 5 (Month 5):** Policy development and employee training  
**Phase 6 (Month 6):** Security assessment and compliance validation

## Budget Considerations

Based on their size and needs, they're looking at roughly:
- Security software licenses: $15,000-20,000/year
- Hardware (firewall, switches): $10,000-15,000 one-time
- Professional services: $25,000-30,000 for setup
- Ongoing managed services: $5,000-8,000/month

## Conclusion

Look, Widget World Inc. is in a pretty good position because they're thinking about security before they have a major incident. The plan I've outlined here should give them solid protection for their customer data while allowing their business to keep growing. The key is implementing everything in phases and making sure their employees actually follow the policies we put in place.

The most critical parts are the customer payment data protection and the employee training - you can have the best technology in the world, but if someone clicks on a phishing email, you're still in trouble.

---
*Prepared by: [Student Name]*  
*Date: [Current Date]*  
*Course: Creating a Company Culture for Security*