## Powershell

# List of subfolders until depth 2
```
Get-ChildItem -Recurse -Directory -Depth 2 . | Select-Object -Property FullName
```

# Retrieve CAL's Used By User
```
Get-ADUser -Filter {(msTSManagingLS -like '*' -and msTSLicenseVersion -like '*') -or (terminalServer -like '*')} -Properties * | ft -Property Name,sAMAccountName,msTS* 
```


# Total Users in Active Directory
```
Get-ADUser -filter * | FT -Property GivenName,Surname,sAMAccountName
```

# Export Users from Active Directory
```
GET-AdUser -Filter * -Properties * | FT -Property GivenName,SurName,sAMAccountName,CN,Description, enabled > c:\adusers.txt
```

#Copy users from one security group to another security group
```
Get-ADGroupMember -Identity GROUP-Source | Add-ADPrincipalGroupMembership -MemberOf GROUP-Destiny
```

#list members of Security Group 
```
Get-ADGroupMember -Identity GROUP-Source 
```

#Tmux

ctrl+b

c	create
,	rename window
p	previous
n	next
w	list windows
%	vertical split
:	split window
d	detach

