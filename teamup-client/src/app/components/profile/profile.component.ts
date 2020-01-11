import { Component, OnInit } from '@angular/core';
import { UserService } from 'src/app/core/services/user/user.service';
import { FormBuilder, Validators, FormGroup, FormControl } from '@angular/forms';
import { User } from '../../shared/models/user';
import { UserSkill } from '../../shared/models/userSkill';
import { ProjectExperience } from '../../shared/models/projectExperience';
import { Technology } from '../../shared/models/technology';
import { Project } from '../../shared/models/project';
import { UserExperience } from '../../shared/models/userExperience';
import {UserWithPictureWrapper} from "../../shared/models/userWithPictureWrapper";
import {MatSnackBar} from "@angular/material";

@Component({
  selector: 'teamup-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  //wrapped containing user object and its profile picture
  currentLoggedInUserWrapped: any;
  //logged in user object
  currentLoggedInUser: any;
  //logged in user's profile picture
  profilePicture: string;
  profileForm: FormGroup;

  constructor(private userService: UserService, formBuilder: FormBuilder, private snackbar: MatSnackBar) {
    this.profileForm = formBuilder.group({
      basicInfo: new FormControl('', [Validators.required]),
      technicalInfo: new FormControl('', [Validators.required]),
    });
  }

  ngOnInit() {
    if (localStorage.getItem('currentUser')) {
      let currentLoggedInUserName = JSON.parse(localStorage.getItem('currentUser'));
      this.userService.getUserWithPicture('userWithPicture/', currentLoggedInUserName).subscribe(obj => {
        this.currentLoggedInUserWrapped = obj;
        //retrieve data from user with picture wrapper obj
        this.currentLoggedInUser = this.currentLoggedInUserWrapped.userToUpdate;
        this.profilePicture = this.currentLoggedInUserWrapped.profilePicture;
      });
    }
  }

  public onSubmit() {
    const basicInfoFormValue = this.profileForm.get('basicInfo').value;
    const technicalInfoFormValue = this.profileForm.get('technicalInfo').value;

    const user: User = {
      id: 0,
      username: null,
      password: null,
      email: basicInfoFormValue.email,
      firstName: basicInfoFormValue.firstName,
      lastName: basicInfoFormValue.lastName,
      birthDate: basicInfoFormValue.birthDate,
      picture: basicInfoFormValue.picture.blob,
      language: basicInfoFormValue.language,
      role: null,
      seniority: this.profileForm.get('technicalInfo').value.seniority.seniority,
      location: technicalInfoFormValue.location.location,
      company: {
        id: technicalInfoFormValue.company.company.id ? technicalInfoFormValue.company.company.id : 0,
        name: technicalInfoFormValue.company.company.name ? technicalInfoFormValue.company.company.name : technicalInfoFormValue.company.company
      },
      skills: null,
      projectExperiences: null,
    };

    const skills = this.profileForm.get('technicalInfo').value.skills;
    const skillArray: UserSkill[] = [];

    skills.forEach(skill => {
      const technology: Technology = {
        id: 0,
        name: skill.technology.techName,
        area: {
          id: skill.technology.techArea.techArea.id ? skill.technology.techArea.techArea.id : 0,
          name: skill.technology.techArea.techArea.name ? skill.technology.techArea.techArea.name : skill.technology.techArea.techArea
        }
      };

      const skillLevel = skill.skillLevel.skillLevel;

      const userSkill: UserSkill = {
        id: 0,
        technology: technology,
        level: skillLevel
      };
      skillArray.push(userSkill);
    });

    user.skills = skillArray;

    const projectExperiences = this.profileForm.get('technicalInfo').value.projectExperiences;
    const projectExperiencesArray: ProjectExperience[] = [];

    projectExperiences.forEach(projectExp => {
      const userExperienceArray: UserExperience[] = [];
      const userExperiences = projectExp.project.userExperience;

      userExperiences.forEach(userExp => {
        userExperienceArray.push(userExp);
      });

      const project: Project = {
        id: 0,
        name: projectExp.project.name,
        description: projectExp.project.description,
        industry: {
          id: projectExp.project.industry.industry.id ? projectExp.project.industry.industry.id : 0,
          name: projectExp.project.industry.industry.name ? projectExp.project.industry.industry.name : projectExp.project.industry.industry,
        },
        company: {
          id: projectExp.project.company.company.id ? projectExp.project.company.company.id : 0,
          name: projectExp.project.company.company.name ? projectExp.project.company.company.name : projectExp.project.company.company
        },
        userExperience: userExperienceArray
      };

      const projectExperience: ProjectExperience = {
        project: project,
        startDate: projectExp.startDate,
        endDate: projectExp.endDate,
        description: projectExp.description
      };
      projectExperiencesArray.push(projectExperience);
    });
    user.projectExperiences = projectExperiencesArray;

    //workaround for user with profile picture
    user.id = this.currentLoggedInUser.id;
    const profilePicture = user.picture;
    user.picture = null;
    const userToBeUpdated: UserWithPictureWrapper = {
      userToUpdate: user,
      profilePicture: profilePicture,
    };

    this.userService.update('update', userToBeUpdated).subscribe(
        () => {
          this.snackbar.open('User saved.');
        },
        () => {
          this.snackbar.open('Error occured.');
        }
    );

  }
}
